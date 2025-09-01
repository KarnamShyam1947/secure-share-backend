<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Reset Your SecureShare Password</title>
</head>
<body style="margin:0;padding:0;width:100%;background-color:#E5D0FF;font-family:Segoe UI, Arial, sans-serif;color:#333333;line-height:1.5;">
  <table width="100%" cellpadding="0" cellspacing="0" border="0" style="padding:20px;">
    <tr>
      <td align="center">
        <table width="600" cellpadding="0" cellspacing="0" border="0" style="background-color:#FFFFFF;border-radius:8px;overflow:hidden;">
          <tr>
            <td align="center" style="background-color:#CCA3FF;padding:20px;">
              <h1 style="margin:0;color:#FFFFFF;font-size:24px;font-family:Segoe UI, Arial, sans-serif;">
                SecureShare Password Reset
              </h1>
            </td>
          </tr>
          
          <tr>
            <td style="padding:30px 20px;font-family:Segoe UI, Arial, sans-serif;color:#333333;">
              <h2 style="color:#BF8BFF;font-size:20px;margin-bottom:10px;">
                Hi ${userName},
              </h2>
              <p style="font-size:16px;margin-bottom:20px;">
                We received a request to reset the password for your SecureShare account. Click the button below to set a new password.
              </p>
              <p style="font-size:16px;margin-bottom:20px;color:#BF8BFF;font-weight:bold;">
                Note: This link will expire in 24 hours.
              </p>
              <div style="text-align:center;margin-bottom:20px;">
                <a href="${link}" style="display:inline-block;background-color:#BF8BFF;color:#FFFFFF;text-decoration:none;padding:12px 24px;border-radius:4px;font-size:16px;">
                  Reset Password
                </a>
              </div>
              <p style="font-size:16px;margin-bottom:20px;">
                If the button doesn’t work, copy and paste the following URL into your browser:
              </p>
              <p style="font-size:16px;margin-bottom:20px;word-break:break-all;">
                <a href="${link}" style="color:#7B1FA2;">${link}</a>
              </p>
              <p style="font-size:16px;margin-bottom:20px;">
                If you didn’t request a password reset, you can safely ignore this email.
              </p>
              <p style="font-size:16px;margin-bottom:0;">
                Cheers,<br>The SecureShare Team
              </p>
            </td>
          </tr>
          
          <tr>
            <td style="background-color:#DABCFF;padding:15px 20px;font-size:12px;text-align:center;color:#666666;font-family:Segoe UI, Arial, sans-serif;">
              © ${currentYear} SecureShare. All rights reserved.<br>
              This password reset link expires in 24 hours.
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
