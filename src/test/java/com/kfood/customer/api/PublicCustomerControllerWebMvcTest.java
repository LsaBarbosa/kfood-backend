package com.kfood.customer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kfood.customer.app.CustomerIdentifierConflictException;
import com.kfood.customer.app.UpsertCustomerUseCase;
import com.kfood.merchant.app.StoreSlugNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "app.security.jwt-secret=12345678901234567890123456789012",
      "app.security.jwt-expiration-seconds=3600"
    })
class PublicCustomerControllerWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UpsertCustomerUseCase upsertCustomerUseCase;

  @Test
  void shouldUpsertCustomerWithoutAuthentication() throws Exception {
    var customerId = UUID.randomUUID();
    when(upsertCustomerUseCase.execute(eq("loja-do-bairro"), any(UpsertCustomerRequest.class)))
        .thenReturn(
            new CustomerResponse(
                customerId, "Maria Silva", "21999990000", "maria@email.com", null));

    mockMvc
        .perform(
            post("/v1/public/stores/loja-do-bairro/customers")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Maria Silva",
                      "phone": "21999990000",
                      "email": "maria@email.com"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(customerId.toString()))
        .andExpect(jsonPath("$.name").value("Maria Silva"));
  }

  @Test
  void shouldUpsertCustomerWithAddressAndReturnMainAddressId() throws Exception {
    var customerId = UUID.randomUUID();
    var mainAddressId = UUID.randomUUID();
    when(upsertCustomerUseCase.execute(eq("loja-do-bairro"), any(UpsertCustomerRequest.class)))
        .thenReturn(
            new CustomerResponse(
                customerId, "Maria Silva", "21999990000", "maria@email.com", mainAddressId));

    mockMvc
        .perform(
            post("/v1/public/stores/loja-do-bairro/customers")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Maria Silva",
                      "phone": "21999990000",
                      "email": "maria@email.com",
                      "address": {
                        "label": "Casa",
                        "zipCode": "25000-000",
                        "street": "Rua das Flores",
                        "number": "45",
                        "district": "Centro",
                        "city": "Mage",
                        "state": "RJ",
                        "mainAddress": true
                      }
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(customerId.toString()))
        .andExpect(jsonPath("$.mainAddressId").value(mainAddressId.toString()));
  }

  @Test
  void shouldReturnNotFoundWhenStoreSlugDoesNotExist() throws Exception {
    when(upsertCustomerUseCase.execute(eq("nao-existe"), any(UpsertCustomerRequest.class)))
        .thenThrow(new StoreSlugNotFoundException("nao-existe"));

    mockMvc
        .perform(
            post("/v1/public/stores/nao-existe/customers")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Maria Silva",
                      "phone": "21999990000"
                    }
                    """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void shouldReturnConflictWhenIdentifiersBelongToDifferentCustomers() throws Exception {
    when(upsertCustomerUseCase.execute(eq("loja-do-bairro"), any(UpsertCustomerRequest.class)))
        .thenThrow(new CustomerIdentifierConflictException());

    mockMvc
        .perform(
            post("/v1/public/stores/loja-do-bairro/customers")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Maria Silva",
                      "phone": "21999990000",
                      "email": "maria@email.com"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void shouldReturnBadRequestWhenNameIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/v1/public/stores/loja-do-bairro/customers")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": " ",
                      "phone": "21999990000"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("name"));
  }

  @Test
  void shouldReturnBadRequestWhenZipCodeIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/v1/public/stores/loja-do-bairro/customers")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Maria Silva",
                      "phone": "21999990000",
                      "address": {
                        "label": "Casa",
                        "zipCode": "2500-000",
                        "street": "Rua das Flores",
                        "number": "45",
                        "district": "Centro",
                        "city": "Mage",
                        "state": "RJ",
                        "mainAddress": true
                      }
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("address.zipCode"));
  }

  @Test
  void shouldReturnBadRequestWhenAddressFieldsAreMissing() throws Exception {
    mockMvc
        .perform(
            post("/v1/public/stores/loja-do-bairro/customers")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Maria Silva",
                      "phone": "21999990000",
                      "address": {
                        "label": "Casa",
                        "zipCode": "25000-000"
                      }
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
