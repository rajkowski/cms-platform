/*
 * Copyright 2024 Matt Rajkowski (https://www.github.com/rajkowski)
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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses a JWT and validates its JSON header and claims payload
 *
 * @author matt rajkowski
 * @created 4/9/24 9:19 PM
 */
public class JWTCommand {

  private static Log LOG = LogFactory.getLog(JWTCommand.class);
  private static final ObjectMapper MAPPER = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  public static JsonNode parseVerifiedJwt(String jwt) {
    JsonNode jwks = OAuthConfigurationCommand.retrieveJwks();
    String sharedSecret = OAuthConfigurationCommand.getClientSecret();
    boolean verified = verifySignature(jwt, jwks, sharedSecret);
    if (!verified && signingKeyIsMissing(jwt, jwks)) {
      verified = verifySignature(jwt, OAuthConfigurationCommand.retrieveJwks(true), sharedSecret);
    }
    if (!verified) {
      LOG.warn("JWT signature could not be verified");
      return null;
    }
    return parseJwt(jwt);
  }

  public static boolean verifySignature(String jwt, JsonNode jwks) {
    return verifySignature(jwt, jwks, null);
  }

  public static boolean verifySignature(String jwt, JsonNode jwks, String sharedSecret) {
    String[] chunks = splitJwt(jwt);
    if (chunks == null) {
      return false;
    }
    try {
      Base64.Decoder decoder = Base64.getUrlDecoder();
      JsonNode header = MAPPER.readTree(new String(decoder.decode(chunks[0]), StandardCharsets.UTF_8));
      if (header == null || !header.isObject() || !header.hasNonNull("alg")) {
        return false;
      }
      String algorithm = header.get("alg").asText();
      byte[] signingInput = (chunks[0] + "." + chunks[1]).getBytes(StandardCharsets.US_ASCII);
      byte[] signature = decoder.decode(chunks[2]);

      if (algorithm.startsWith("HS")) {
        return verifyHmac(algorithm, signingInput, signature, header, jwks, sharedSecret);
      }
      JsonNode key = findSigningKey(jwks, header);
      if (key == null) {
        return false;
      }
      String keyAlgorithm = key.path("alg").asText();
      if (StringUtils.isNotBlank(keyAlgorithm) && !algorithm.equals(keyAlgorithm)) {
        return false;
      }
      PublicKey publicKey = createPublicKey(key);
      if (publicKey == null) {
        return false;
      }
      return verifyAsymmetricSignature(algorithm, signingInput, signature, publicKey);
    } catch (IllegalArgumentException | GeneralSecurityException | JsonProcessingException exception) {
      LOG.debug("Unable to verify JWT signature", exception);
      return false;
    }
  }

  public static JsonNode parseJwt(String jwt) {
    String[] chunks = splitJwt(jwt);
    if (chunks == null) {
      return null;
    }

    try {
      Base64.Decoder decoder = Base64.getUrlDecoder();
      String header = new String(decoder.decode(chunks[0]), StandardCharsets.UTF_8);
      String payload = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);
      JsonNode headerJson = MAPPER.readTree(header);
      JsonNode payloadJson = MAPPER.readTree(payload);
      if (headerJson == null || !headerJson.isObject() || payloadJson == null || !payloadJson.isObject()) {
        LOG.debug("JWT header and payload must be JSON objects");
        return null;
      }
      return payloadJson;
    } catch (IllegalArgumentException | JsonProcessingException exception) {
      LOG.debug("Unable to parse JWT", exception);
      return null;
    }
  }

  private static String[] splitJwt(String jwt) {
    if (StringUtils.isBlank(jwt) || !jwt.contains(".")) {
      LOG.debug("String does not appear to be a JWT");
      return null;
    }
    String[] chunks = jwt.split("\\.", -1);
    if (chunks.length != 3 || StringUtils.isAnyBlank(chunks)) {
      LOG.debug("String does not have JWT chunks");
      return null;
    }
    return chunks;
  }

  private static JsonNode findSigningKey(JsonNode jwks, JsonNode header) {
    if (jwks == null || !jwks.has("keys") || !jwks.get("keys").isArray()) {
      return null;
    }
    String keyId = header.path("kid").asText(null);
    String thumbprint = header.path("x5t").asText(null);
    JsonNode matchingKey = null;
    for (JsonNode key : jwks.get("keys")) {
      if (StringUtils.isNotBlank(key.path("use").asText()) && !"sig".equals(key.path("use").asText())) {
        continue;
      }
      boolean matchesKeyId = StringUtils.isNotBlank(keyId) && keyId.equals(key.path("kid").asText());
      boolean matchesThumbprint = StringUtils.isNotBlank(thumbprint) && thumbprint.equals(key.path("x5t").asText());
      if ((StringUtils.isBlank(keyId) && StringUtils.isBlank(thumbprint)) || matchesKeyId || matchesThumbprint) {
        if (matchingKey != null) {
          return null;
        }
        matchingKey = key;
      }
    }
    return matchingKey;
  }

  private static boolean signingKeyIsMissing(String jwt, JsonNode jwks) {
    String[] chunks = splitJwt(jwt);
    if (chunks == null) {
      return false;
    }
    try {
      JsonNode header = MAPPER.readTree(new String(Base64.getUrlDecoder().decode(chunks[0]), StandardCharsets.UTF_8));
      if (header == null || !header.isObject()
          || StringUtils.isAllBlank(header.path("kid").asText(), header.path("x5t").asText())) {
        return false;
      }
      return findSigningKey(jwks, header) == null;
    } catch (IllegalArgumentException | JsonProcessingException exception) {
      return false;
    }
  }

  private static PublicKey createPublicKey(JsonNode key) throws GeneralSecurityException {
    String keyType = key.path("kty").asText();
    if ("RSA".equals(keyType)) {
      BigInteger modulus = unsignedBigInteger(key.path("n").asText());
      BigInteger exponent = unsignedBigInteger(key.path("e").asText());
      return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }
    if ("EC".equals(keyType)) {
      String curve = switch (key.path("crv").asText()) {
      case "P-256" -> "secp256r1";
      case "P-384" -> "secp384r1";
      case "P-521" -> "secp521r1";
      default -> null;
      };
      if (curve == null) {
        return null;
      }
      AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
      parameters.init(new ECGenParameterSpec(curve));
      ECPoint point = new ECPoint(unsignedBigInteger(key.path("x").asText()), unsignedBigInteger(key.path("y").asText()));
      return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, parameters.getParameterSpec(java.security.spec.ECParameterSpec.class)));
    }
    return null;
  }

  private static boolean verifyAsymmetricSignature(String algorithm, byte[] signingInput, byte[] jwtSignature, PublicKey publicKey)
      throws GeneralSecurityException {
    Signature verifier = switch (algorithm) {
    case "RS256" -> Signature.getInstance("SHA256withRSA");
    case "RS384" -> Signature.getInstance("SHA384withRSA");
    case "RS512" -> Signature.getInstance("SHA512withRSA");
    case "PS256" -> createPssSignature("SHA-256");
    case "PS384" -> createPssSignature("SHA-384");
    case "PS512" -> createPssSignature("SHA-512");
    case "ES256" -> Signature.getInstance("SHA256withECDSA");
    case "ES384" -> Signature.getInstance("SHA384withECDSA");
    case "ES512" -> Signature.getInstance("SHA512withECDSA");
    default -> null;
    };
    if (verifier == null || !isAlgorithmCompatible(algorithm, publicKey)) {
      return false;
    }
    if (algorithm.startsWith("ES")) {
      jwtSignature = joseToDerSignature(jwtSignature, algorithm);
      if (jwtSignature == null) {
        return false;
      }
    }
    verifier.initVerify(publicKey);
    verifier.update(signingInput);
    return verifier.verify(jwtSignature);
  }

  private static boolean verifyHmac(String algorithm, byte[] signingInput, byte[] signature, JsonNode header, JsonNode jwks,
      String sharedSecret) throws GeneralSecurityException {
    String hmacAlgorithm = switch (algorithm) {
    case "HS256" -> "HmacSHA256";
    case "HS384" -> "HmacSHA384";
    case "HS512" -> "HmacSHA512";
    default -> null;
    };
    if (hmacAlgorithm == null) {
      return false;
    }
    byte[] key = null;
    JsonNode signingKey = findSigningKey(jwks, header);
    if (signingKey != null && "oct".equals(signingKey.path("kty").asText())) {
      try {
        key = Base64.getUrlDecoder().decode(signingKey.path("k").asText());
      } catch (IllegalArgumentException exception) {
        return false;
      }
    } else if (StringUtils.isNotBlank(sharedSecret)) {
      key = sharedSecret.getBytes(StandardCharsets.UTF_8);
    }
    if (key == null) {
      return false;
    }
    Mac mac = Mac.getInstance(hmacAlgorithm);
    mac.init(new SecretKeySpec(key, hmacAlgorithm));
    return MessageDigest.isEqual(signature, mac.doFinal(signingInput));
  }

  private static Signature createPssSignature(String digest) throws GeneralSecurityException {
    int digestLength = switch (digest) {
    case "SHA-256" -> 32;
    case "SHA-384" -> 48;
    case "SHA-512" -> 64;
    default -> 0;
    };
    Signature signature = Signature.getInstance("RSASSA-PSS");
    signature.setParameter(new PSSParameterSpec(digest, "MGF1", new MGF1ParameterSpec(digest), digestLength, 1));
    return signature;
  }

  private static boolean isAlgorithmCompatible(String algorithm, PublicKey key) {
    if ((algorithm.startsWith("RS") || algorithm.startsWith("PS")) && "RSA".equals(key.getAlgorithm())) {
      return true;
    }
    if (!algorithm.startsWith("ES") || !key.getAlgorithm().equals("EC")) {
      return false;
    }
    int keySize = ((ECPublicKey) key).getParams().getOrder().bitLength();
    return algorithm.equals("ES256") && keySize <= 256
        || algorithm.equals("ES384") && keySize > 256 && keySize <= 384
        || algorithm.equals("ES512") && keySize > 384;
  }

  private static BigInteger unsignedBigInteger(String encoded) {
    if (StringUtils.isBlank(encoded)) {
      throw new IllegalArgumentException("JWK value is required");
    }
    return new BigInteger(1, Base64.getUrlDecoder().decode(encoded));
  }

  private static byte[] joseToDerSignature(byte[] signature, String algorithm) {
    int componentLength = switch (algorithm) {
    case "ES256" -> 32;
    case "ES384" -> 48;
    case "ES512" -> 66;
    default -> 0;
    };
    if (componentLength == 0 || signature.length != componentLength * 2) {
      return null;
    }
    byte[] r = encodeDerInteger(signature, 0, componentLength);
    byte[] s = encodeDerInteger(signature, componentLength, componentLength);
    ByteArrayOutputStream encoded = new ByteArrayOutputStream(r.length + s.length + 5);
    encoded.write(0x30);
    int contentLength = r.length + s.length;
    if (contentLength < 0x80) {
      encoded.write(contentLength);
    } else {
      encoded.write(0x81);
      encoded.write(contentLength);
    }
    encoded.writeBytes(r);
    encoded.writeBytes(s);
    return encoded.toByteArray();
  }

  private static byte[] encodeDerInteger(byte[] value, int valueOffset, int length) {
    while (length > 1 && value[valueOffset] == 0) {
      valueOffset++;
      length--;
    }
    boolean addPadding = (value[valueOffset] & 0x80) != 0;
    byte[] encoded = new byte[length + 2 + (addPadding ? 1 : 0)];
    encoded[0] = 0x02;
    encoded[1] = (byte) (length + (addPadding ? 1 : 0));
    if (addPadding) {
      encoded[2] = 0;
    }
    System.arraycopy(value, valueOffset, encoded, addPadding ? 3 : 2, length);
    return encoded;
  }
}
