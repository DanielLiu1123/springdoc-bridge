package example;

import com.example.payment.v1.CreatePaymentRequest;
import com.example.payment.v1.CreatePaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller demonstrating how a protobuf {@code oneof} is documented in OpenAPI.
 *
 * <p>The example application enables {@code springdoc-bridge.protobuf.oneof-behavior: one_of},
 * so the {@code payment_method} oneof of {@link CreatePaymentRequest} is rendered as an OpenAPI
 * {@code oneOf} instead of flattened sibling properties.
 *
 * @see <a href="https://github.com/DanielLiu1123/springdoc-bridge/issues/23">Issue #23</a>
 */
@RestController
@Tag(name = "Payment", description = "Demonstrates protobuf oneof -> OpenAPI oneOf mapping")
public class PaymentController {

    @PostMapping("/v1/payments")
    @Operation(
            summary = "Create a payment",
            description = "Creates a payment using exactly one of the supported payment methods")
    public CreatePaymentResponse createPayment(@RequestBody CreatePaymentRequest request) {
        return CreatePaymentResponse.getDefaultInstance();
    }
}
