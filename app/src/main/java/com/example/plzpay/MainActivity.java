package com.example.plzpay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private PaymentsClient paymentsClient;
    Button googlePayButton;
    private JSONObject transactionInfo = new JSONObject();
    private JSONObject tokenizationSpecification = new JSONObject();
    private JSONObject cardPaymentMethod = new JSONObject();
    private JSONObject merchantInfo = new JSONObject();
    private JSONObject paymentDataRequestJson;
    PaymentDataRequest paymentDataRequest;
    private final int LOAD_PAYMENT_DATA_REQUEST_CODE = 101;

    EditText editText;
    String price;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        paymentsClient = createPaymentsClient(this);
        googlePayButton = findViewById(R.id.googlePayButton);
        editText = findViewById(R.id.editTextNumber);

        IsReadyToPayRequest readyToPayRequest = IsReadyToPayRequest.fromJson(googlePayBaseConfiguration.toString());

        Task<Boolean> readyToPayTask = paymentsClient.isReadyToPay(readyToPayRequest);
        readyToPayTask.addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                try {
                    if (task.getResult(ApiException.class) != null) {
                        setGooglePayAvailable(task.getResult(ApiException.class));
                    }
                } catch (ApiException exception) {
                    // Error determining readiness to use Google Pay.
                    // Inspect the logs for more details.
                }
            }
        });

    }

    private PaymentsClient createPaymentsClient(Context context) {
        Wallet.WalletOptions walletOptions = new Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST).build();
        return Wallet.getPaymentsClient(context, walletOptions);
    }

    private JSONObject baseCardPaymentMethod = new JSONObject();

    {
        try {
            baseCardPaymentMethod.put("type", "CARD");

            JSONObject parameters = new JSONObject();
            parameters.put("allowedCardNetworks", new JSONArray(Arrays.asList("VISA", "MASTERCARD")));
            parameters.put("allowedAuthMethods", new JSONArray(Arrays.asList("PAN_ONLY", "CRYPTOGRAM_3DS")));

            baseCardPaymentMethod.put("parameters", parameters);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject googlePayBaseConfiguration = new JSONObject();

    {
        try {
            googlePayBaseConfiguration.put("apiVersion", 2);
            googlePayBaseConfiguration.put("apiVersionMinor", 0);

            JSONArray allowedPaymentMethods = new JSONArray();
            allowedPaymentMethods.put(baseCardPaymentMethod);

            googlePayBaseConfiguration.put("allowedPaymentMethods", allowedPaymentMethods);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setGooglePayAvailable(Boolean available) {
        if (available) {
            googlePayButton.setVisibility(View.VISIBLE);
            googlePayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    price = editText.getText().toString();
                    requestPayment();
                }
            });
        } else {
            // Unable to pay using Google Pay. Update your UI accordingly.
        }
    }

    private void requestPayment() {
        try {
            tokenizationSpecification.put("type", "PAYMENT_GATEWAY");
            JSONObject parameters = new JSONObject();
            parameters.put("gateway", "example");
            parameters.put("gatewayMerchantId", "exampleGatewayMerchantId");
            tokenizationSpecification.put("parameters", parameters);
            try {
                transactionInfo.put("totalPrice", price);
                transactionInfo.put("totalPriceStatus", "FINAL");
                transactionInfo.put("currencyCode", "NIS");

                try {
                    cardPaymentMethod.put("type", "CARD");
                    cardPaymentMethod.put("tokenizationSpecification", tokenizationSpecification);
                    JSONObject parameters2 = new JSONObject();
                    parameters2.put("allowedCardNetworks", new JSONArray(Arrays.asList("VISA", "MASTERCARD")));
                    parameters2.put("allowedAuthMethods", new JSONArray(Arrays.asList("PAN_ONLY", "CRYPTOGRAM_3DS")));
                    parameters2.put("billingAddressRequired", true);
                    JSONObject billingAddressParameters = new JSONObject();
                    billingAddressParameters.put("format", "FULL");
                    parameters2.put("billingAddressParameters", billingAddressParameters);
                    cardPaymentMethod.put("parameters", parameters2);

                    try {
                        merchantInfo.put("merchantName", "Example Merchant");
                        merchantInfo.put("merchantId", "01234567890123456789");

                        try {
                            paymentDataRequestJson = new JSONObject(googlePayBaseConfiguration.toString());
                            paymentDataRequestJson.put("allowedPaymentMethods", new JSONArray().put(cardPaymentMethod));
                            paymentDataRequestJson.put("transactionInfo", transactionInfo);
                            paymentDataRequestJson.put("merchantInfo", merchantInfo);

                            try {
                                paymentDataRequest = PaymentDataRequest.fromJson(paymentDataRequestJson.toString());
                                AutoResolveHelper.resolveTask(
                                        paymentsClient.loadPaymentData(paymentDataRequest),
                                        this,
                                        LOAD_PAYMENT_DATA_REQUEST_CODE);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    PaymentData paymentData = PaymentData.getFromIntent(data);
                    if (paymentData != null) {
                        handlePaymentSuccess(paymentData);
                    }
                    break;

                case Activity.RESULT_CANCELED:
                    // The user cancelled without selecting a payment method.
                    break;

                case AutoResolveHelper.RESULT_ERROR:
                    Status status = AutoResolveHelper.getStatusFromIntent(data);
                    if (status != null) {
                        Toast.makeText(this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                    }
                    break;

                default:
                    // Unexpected resultCode.
                    break;
            }
        }
    }

    private void handlePaymentSuccess(PaymentData paymentData) {
        try {
            JSONObject tokenizationData = new JSONObject(paymentData.toJson())
                    .getJSONObject("tokenizationData");
            String paymentMethodToken = tokenizationData.getString("token");

            // Sample TODO: Use this token to perform a payment through your payment gateway
        } catch (JSONException e) {
            // Handle JSON parsing error
        }
    }


}