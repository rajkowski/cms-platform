/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.simisinc.platform.application.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;

class JWTCommandTest {

  @Test
  void parsesObjectPayload() {
    JsonNode payload = JWTCommand.parseJwt(jwt("{\"sub\":\"user-1\"}"));

    assertEquals("user-1", payload.get("sub").asText());
  }

  @Test
  void rejectsInvalidOrNonObjectPayload() {
    assertNull(JWTCommand.parseJwt(jwt("{invalid")));
    assertNull(JWTCommand.parseJwt(jwt("[]")));
    assertNull(JWTCommand.parseJwt(jwt("{} trailing")));
  }

  @Test
  void rejectsInvalidJwtSegments() {
    String token = jwt("{\"sub\":\"user-1\"}");

    assertNull(JWTCommand.parseJwt(token.substring(0, token.lastIndexOf('.'))));
    assertNull(JWTCommand.parseJwt(token.substring(0, token.indexOf('.') + 1) + "%%%." + token.substring(token.lastIndexOf('.') + 1)));
  }

  @Test
  void verifiesRsaSignature() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    String token = rsaJwt("{\"alg\":\"RS256\",\"kid\":\"test-key\"}", "{\"sub\":\"user-1\"}", keyPair);
    JsonNode jwks = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rsaJwks((RSAPublicKey) keyPair.getPublic()));

    assertTrue(JWTCommand.verifySignature(token, jwks));
    int signatureStart = token.lastIndexOf('.') + 1;
    char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
    String tamperedToken = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
    assertFalse(JWTCommand.verifySignature(tamperedToken, jwks));
  }

  @Test
  void refreshesJwksWhenSigningKeyIsMissing() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    String token = rsaJwt("{\"alg\":\"RS256\",\"kid\":\"test-key\"}", "{\"sub\":\"user-1\"}", keyPair);
    JsonNode staleJwks = new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"keys\":[]}");
    JsonNode currentJwks = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rsaJwks((RSAPublicKey) keyPair.getPublic()));

    try (MockedStatic<OAuthConfigurationCommand> configuration = Mockito.mockStatic(OAuthConfigurationCommand.class)) {
      configuration.when(OAuthConfigurationCommand::retrieveJwks).thenReturn(staleJwks);
      configuration.when(() -> OAuthConfigurationCommand.retrieveJwks(true)).thenReturn(currentJwks);
      configuration.when(OAuthConfigurationCommand::getClientSecret).thenReturn(null);

      assertEquals("user-1", JWTCommand.parseVerifiedJwt(token).get("sub").asText());
      configuration.verify(() -> OAuthConfigurationCommand.retrieveJwks(true));
    }
  }

  @Test
  void doesNotRefreshJwksForInvalidSignature() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    String token = rsaJwt("{\"alg\":\"RS256\",\"kid\":\"test-key\"}", "{\"sub\":\"user-1\"}", keyPair);
    int signatureStart = token.lastIndexOf('.') + 1;
    char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
    String tamperedToken = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
    JsonNode jwks = new com.fasterxml.jackson.databind.ObjectMapper().readTree(rsaJwks((RSAPublicKey) keyPair.getPublic()));

    try (MockedStatic<OAuthConfigurationCommand> configuration = Mockito.mockStatic(OAuthConfigurationCommand.class)) {
      configuration.when(OAuthConfigurationCommand::retrieveJwks).thenReturn(jwks);
      configuration.when(OAuthConfigurationCommand::getClientSecret).thenReturn(null);

      assertNull(JWTCommand.parseVerifiedJwt(tamperedToken));
      configuration.verify(() -> OAuthConfigurationCommand.retrieveJwks(true), Mockito.never());
    }
  }

  @Test
  void rejectsUnsignedAndUnknownAlgorithmTokens() throws Exception {
    JsonNode jwks = new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"keys\":[]}");

    assertFalse(JWTCommand.verifySignature(jwt("{\"sub\":\"user-1\"}"), jwks));
    assertFalse(JWTCommand.verifySignature(token("{\"alg\":\"unknown\"}", "{\"sub\":\"user-1\"}"), jwks));
  }

  private static String rsaJwt(String header, String payload, KeyPair keyPair) throws Exception {
    String signingInput = encode(header) + "." + encode(payload);
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(keyPair.getPrivate());
    signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
    return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
  }

  private static String rsaJwks(RSAPublicKey publicKey) {
    return "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"test-key\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"" + encode(publicKey.getModulus())
        + "\",\"e\":\"" + encode(publicKey.getPublicExponent()) + "\"}]}";
  }

  private static String jwt(String payload) {
    return token("{\"alg\":\"none\"}", payload);
  }

  private static String token(String header, String payload) {
    return encode(header) + "." + encode(payload) + "." + encode("signature");
  }

  private static String encode(String value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String encode(BigInteger value) {
    byte[] bytes = value.toByteArray();
    if (bytes[0] == 0) {
      bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
