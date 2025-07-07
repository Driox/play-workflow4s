package workflow.domain

import zio.json.*

enum KycStatus derives JsonDecoder, JsonEncoder {
  case PendingInitialData
  case IncompleteData
  case WaitingValidation
  case AdditionalInfoRequested
  case Validated
  case Refused
  case KycApproved
  case KycRejected
  case KycUpdateRequired
}

enum KycKind derives JsonDecoder, JsonEncoder {
  case Person
  case Company
}

enum KycDocumentType derives JsonDecoder, JsonEncoder {
  case Passport
  case IdCard
  case ProofOfAddress
  case DriverLicence
}
