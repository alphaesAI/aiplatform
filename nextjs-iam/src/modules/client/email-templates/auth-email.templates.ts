export function getEmailVerificationTemplate(
  link: string,
  name: string,
  companyName: string
) {
  return `
  <!DOCTYPE html>
  <html lang="en">
  <head>
    <meta charset="UTF-8" />
    <title>Verify Your Email</title>
  </head>
  <body style="margin:0; padding:0; background-color:#f4f4f7; font-family:Arial, sans-serif;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f4f7; padding:20px 0;">
      <tr>
        <td align="center">
          <table width="500" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:8px; padding:32px; border:1px solid #eaeaea;">

            <tr>
              <td style="font-size:24px; font-weight:bold; padding-bottom:20px;">
                Verify Your Email Address
              </td>
            </tr>

            <tr>
              <td style="font-size:15px; line-height:22px; padding-bottom:20px;">
                Hi ${name},
              </td>
            </tr>

            <tr>
              <td style="font-size:15px; line-height:22px; padding-bottom:20px;">
                Thank you for signing up! To complete your registration, please verify your email address by clicking the button below:
              </td>
            </tr>

            <tr>
              <td align="center" style="padding-bottom:30px;">
                <a href="${link}"
                   style="background:#0070f3; color:#ffffff; text-decoration:none; padding:12px 20px; border-radius:6px; font-size:16px; display:inline-block;">
                  Verify Email
                </a>
              </td>
            </tr>

            <tr>
              <td style="border-top:1px solid #eaeaea; padding-top:20px; font-size:13px; color:#555;">
                If you did not create this account, you can safely ignore this message.
              </td>
            </tr>

            <tr>
              <td style="font-size:13px; color:#555; padding-top:10px;">
                Thanks,<br />${companyName}.
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
  </html>
    `
}
