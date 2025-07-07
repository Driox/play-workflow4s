package workflow.domain

import java.time.{ Instant, LocalDate }
import utils.StringUtils

case class User(
  id:    String = "usr-" + StringUtils.randomAlphanumericString(4),
  email: String
)

case class KycAddress(
  number:           String,
  street:           String,
  postal_code:      String,
  city:             String,
  country:          String,
  proof_of_address: Option[Document] = None
)

case class Document(
  document_type:   KycDocumentType,
  file_url:        String,
  expiration_date: Option[LocalDate]
)

case class KycDataInput(
  email:   Option[String]     = None,
  phone:   Option[String]     = None,
  address: Option[KycAddress] = None,

  // Person
  first_name:   Option[String]    = None,
  last_name:    Option[String]    = None,
  birth_date:   Option[LocalDate] = None,
  birth_place:  Option[String]    = None,
  nationality:  Option[String]    = None,
  id_documents: List[Document]    = List(),
  pep:          Option[Boolean]   = None,
  gad:          Option[Boolean]   = None,

  // RGPD
  consent_given: Option[Boolean] = None,

  // Finances
  occupation:       Option[String]     = None,
  annual_income:    Option[BigDecimal] = None,
  estimated_assets: Option[BigDecimal] = None
)

case class KycData(
  // Meta
  id:         String    = "kyc-" + StringUtils.randomAlphanumericString(4),
  created_at: Instant   = Instant.now(),
  updated_at: Instant   = Instant.now(),
  status:     KycStatus = KycStatus.PendingInitialData,
  user_id:    String,
  kind:       KycKind   = KycKind.Person,

  // contact
  email:   String,
  phone:   Option[String]     = None,
  address: Option[KycAddress] = None,

  // Person
  first_name:   Option[String]    = None,
  last_name:    Option[String]    = None,
  birth_date:   Option[LocalDate] = None,
  birth_place:  Option[String]    = None,
  nationality:  Option[String]    = None,
  id_documents: List[Document]    = List(),
  pep:          Option[Boolean]   = None,
  gad:          Option[Boolean]   = None,

  // RGPD
  consent_given: Boolean = false,

  // Finances
  occupation:       Option[String]     = None,
  annual_income:    Option[BigDecimal] = None,
  estimated_assets: Option[BigDecimal] = None
) {
  def merge(new_data: KycDataInput): KycData = {
    this.copy(
      updated_at       = Instant.now(),
      email            = new_data.email.getOrElse(this.email),
      phone            = new_data.phone.orElse(this.phone),
      address          = new_data.address.orElse(this.address),
      first_name       = new_data.first_name.orElse(this.first_name),
      last_name        = new_data.last_name.orElse(this.last_name),
      birth_date       = new_data.birth_date.orElse(this.birth_date),
      birth_place      = new_data.birth_place.orElse(this.birth_place),
      nationality      = new_data.nationality.orElse(this.nationality),
      pep              = new_data.pep.orElse(this.pep),
      gad              = new_data.gad.orElse(this.gad),
      consent_given    = new_data.consent_given.getOrElse(this.consent_given),
      occupation       = new_data.occupation.orElse(this.occupation),
      annual_income    = new_data.annual_income.orElse(this.annual_income),
      estimated_assets = new_data.estimated_assets.orElse(this.estimated_assets),
      id_documents     = if (new_data.id_documents.nonEmpty) new_data.id_documents else this.id_documents
    )
  }
}
object KycData {
  def apply(user: User, kind: KycKind): KycData = KycData(
    user_id = user.id,
    kind    = kind,
    email   = user.email
  )

  val empty = KycData(
    id      = "kyc-000",
    email   = "empty@empty.com",
    user_id = "usr-000"
  )
}
