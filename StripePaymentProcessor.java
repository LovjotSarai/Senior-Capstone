import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.model.Charge;
import com.stripe.net.RequestOptions;
import com.stripe.net.RequestOptions.RequestOptionsBuilder;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import javax.servlet.http.HttpServletResponse;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class StripePaymentProcessor {
    private String stripeSecretKey;
    private String emailUsername;
    private String emailPassword;
    private HttpServletResponse response;

    public StripePaymentProcessor(String stripeSecretKey, String emailUsername, String emailPassword, HttpServletResponse response) {
        this.stripeSecretKey = stripeSecretKey;
        this.emailUsername = emailUsername;
        this.emailPassword = emailPassword;
        this.response = response;
    }

    public void processPayment(String stripeToken, double amount, String email) throws Exception {
        try {
            // Initialize Stripe API with secret key
            Stripe.apiKey = stripeSecretKey;

            // Create a new Stripe Charge object
            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", (int) (amount * 100));
            chargeParams.put("currency", "usd");
            chargeParams.put("source", stripeToken);
            Charge charge = Charge.create(chargeParams);

            // Generate PDF receipt
            ByteArrayOutputStream receiptStream = generateReceiptPDF(amount);

            // Send payment confirmation email with receipt attachment to the user
            sendEmailWithAttachment(email, "Payment Confirmation and Receipt", "Thank you for your payment of $" + amount + ".", "receipt.pdf", receiptStream.toByteArray());

            // Redirect user to www.canaryconsultants.com on successful payment
            response.sendRedirect("http://www.canaryconsultants.com");

        } catch (CardException e) {
            // Payment was declined by the card issuer
            throw new Exception("Payment failed: " + e.getMessage());

        } catch (Exception e) {
            // Handle any other exceptions that may occur
            throw new Exception("Payment failed: " + e.getMessage());
        }
    }

    private ByteArrayOutputStream generateReceiptPDF(double amount) throws DocumentException {
        // Create new PDF document
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Write receipt details to PDF
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Receipt"));
        document.add(new Paragraph("Amount: $" + amount));
        document.close();

        return out;
    }

    private void sendEmail(String to, String subject, String body) throws MessagingException {
        // Set email properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        // Authenticate with email server
        Authenticator authenticator = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailUsername, emailPassword);
            }
        };
        Session session = Session.getInstance(properties, authenticator);

        // Create new email message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailUsername));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);

        // Send email message
        Transport.send
