import EmailVerification from "@/modules/client/auth/components/auth/EmailVerification"

async function EmailVerificationPage(
  props: PageProps<"/auth/email-verification">
) {
  const searchParams = await props.searchParams
  const email = searchParams?.email as string

  return <EmailVerification email={email} />
}

export default EmailVerificationPage
