package workflow

import zio.json.*

package object domain {

  given adr_decoder: JsonDecoder[KycAddress] = DeriveJsonDecoder.gen[KycAddress]
  given adr_encoder: JsonEncoder[KycAddress] = DeriveJsonEncoder.gen[KycAddress]

  given doc_decoder: JsonDecoder[Document] = DeriveJsonDecoder.gen[Document]
  given doc_encoder: JsonEncoder[Document] = DeriveJsonEncoder.gen[Document]

  given kyc_decoder: JsonDecoder[KycData] = DeriveJsonDecoder.gen[KycData]
  given kyc_encoder: JsonEncoder[KycData] = DeriveJsonEncoder.gen[KycData]
}
