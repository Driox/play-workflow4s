package workflow.domain

import cats.effect.IO
import workflows4s.wio.{ SignalDef, WorkflowContext }

import zio.json.*

trait PekkoJsonSerializable

object KycWorkflow {

  sealed trait KycState
  object KycState {
    type Empty = Empty.type
    case object Empty                                  extends KycState
    case class Ongoing(data: KycData)                  extends KycState
    case class Validated(data: KycData)                extends KycState
    case class Rejected(data: KycData, reason: String) extends KycState
  }

  sealed trait KycEvent
  object KycEvent {
    case class Created(data: KycData)          extends KycEvent with PekkoJsonSerializable
    case class DataUpdated(data: KycData)      extends KycEvent with PekkoJsonSerializable
    case class AddressValidated(data: KycData) extends KycEvent with PekkoJsonSerializable
    case class AddressRefused(reason: String)  extends KycEvent with PekkoJsonSerializable

    implicit val decoder: JsonDecoder[KycEvent] = DeriveJsonDecoder.gen[KycEvent]
    implicit val encoder: JsonEncoder[KycEvent] = DeriveJsonEncoder.gen[KycEvent]
  }

  sealed trait KycSignals
  object Signals {
    val create_kyc: SignalDef[CreateKyc, String]   = SignalDef()
    val update_data: SignalDef[UpdateKyc, KycData] = SignalDef()
    case class CreateKyc(user: User, kind: KycKind) extends KycSignals
    case class UpdateKyc(data: KycDataInput)        extends KycSignals
  }

  sealed trait KycError
  object KycError {
    case class AddressNotValidated(reason: String) extends KycError
    case object IdNotValidated                     extends KycError
    case object UnknwonError                       extends KycError
  }

  object KycContext extends WorkflowContext {
    override type Event = KycEvent
    override type State = KycState
  }
  import KycContext.*

  val start_kyc =
    WIO.handleSignal(Signals.create_kyc)
      .using[Any]
      .purely((in, req) => KycEvent.Created(KycData(req.user, req.kind)))
      .handleEvent((in, evt) => KycState.Ongoing(evt.data))
      .produceResponse((signal, evt) => evt.data.id)
      .autoNamed

  val collect_data =
    WIO.handleSignal(Signals.update_data)
      .using[KycState.Ongoing]
      .purely((in, req) => KycEvent.DataUpdated(in.data.merge(req.data)))
      .handleEvent((in, evt) => KycState.Ongoing(evt.data))
      .produceResponse((signal, evt) => evt.data)
      .autoNamed

  val validate_data =
    WIO.runIO[KycState.Ongoing](in =>
      IO(
        if (in.data.address.flatMap(_.proof_of_address).isDefined) {
          KycEvent.AddressValidated(in.data)
        } else {
          KycEvent.AddressRefused("missing address proof")
        }
      )
    )
      .handleEventWithError((in, evt) =>
        evt match {
          case KycEvent.AddressValidated(data) => Right(KycState.Validated(data))
          case KycEvent.AddressRefused(reason) => Left(KycError.AddressNotValidated(reason))
          case _                               => Left(KycError.UnknwonError)
        }
      )
      .autoNamed

  val valid_kyc =
    WIO.pure.makeFrom[KycState.Validated].value(identity).autoNamed

  val reject_kyc: WIO[(KycState, KycError), Nothing, KycState.Rejected] =
    WIO.pure.makeFrom[(KycState, KycError)]
      .value((state, err) => {
        state match {
          case KycState.Rejected(data, reason) => KycState.Rejected(data, reason)
          case KycState.Ongoing(data)          => KycState.Rejected(data, err.toString())
          case KycState.Validated(data)        => KycState.Rejected(data, err.toString())
          case KycState.Empty                  => KycState.Rejected(KycData.empty, s"Transition not allowed, error $err")
        }
      })
      .autoNamed

  val workflow: WIO.Initial = (
    start_kyc >>>
      collect_data >>>
      validate_data >>>
      valid_kyc
  ).handleErrorWith(reject_kyc)

  val draft = workflow
}
