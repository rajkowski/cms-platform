/*
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

package com.simisinc.platform.application.maps;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.Subdivision;
import com.simisinc.platform.domain.model.maps.GeoIP;

/**
 * Methods to integrate with geo IP services
 *
 * @author matt rajkowski
 * @created 7/2/18 9:14 AM
 */
public class GeoIPCommand {

  private static Log LOG = LogFactory.getLog(GeoIPCommand.class);

  private static DatabaseReader cityReader;
  private static DatabaseReader countryReader;

  // https://dev.maxmind.com/geoip/geoip2/geolite2/

  public static void setConfig(ServletContext context) {
    // Use the City file
    try {
      URL cityDBUrl = context.getResource("/WEB-INF/geo-ip/GeoLite2-City.mmdb");
      cityReader = new DatabaseReader.Builder(new File(cityDBUrl.toURI())).withCache(new CHMCache()).build();
      LOG.info("GeoIP City File Reader created");
    } catch (Exception e) {
      LOG.error("GeoIP City File Reader could not be created: " + e.getMessage());
      // Trying to use InputStream (will be handled by reader)
      try (InputStream inputStream = context.getResourceAsStream("/WEB-INF/geo-ip/GeoLite2-City.mmdb")) {
        cityReader = new DatabaseReader.Builder(inputStream).build();
        LOG.info("City Input Stream created");
      } catch (Exception ie) {
        LOG.error("GeoIP City InputStream could not be created", ie);
      }
    }

    // Use the Country file
    try {
      URL countryDBUrl = context.getResource("/WEB-INF/geo-ip/GeoLite2-Country.mmdb");
      countryReader = new DatabaseReader.Builder(new File(countryDBUrl.toURI())).withCache(new CHMCache()).build();
      LOG.info("GeoIP Country File Reader created");
    } catch (Exception e) {
      LOG.error("GeoIP Country File Reader could not be created: " + e.getMessage());
      // Trying to use InputStream (will be handled by reader)
      try (InputStream inputStream = context.getResourceAsStream("/WEB-INF/geo-ip/GeoLite2-Country.mmdb")) {
        countryReader = new DatabaseReader.Builder(inputStream).build();
        LOG.info("Country Input Stream created");
      } catch (Exception ie) {
        LOG.error("Country GeoIP InputStream could not be created", ie);
      }
    }
  }

  public static GeoIP getLocation(String ip) {
    if (StringUtils.isBlank(ip) || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
      LOG.debug("Skipping for localhost");
      return null;
    }
    if (cityReader == null) {
      return null;
    }
    try {
      LOG.debug("Checking IP: " + ip);
      InetAddress ipAddress = InetAddress.getByName(ip);
      GeoIP geoIP = new GeoIP();
      CityResponse response = cityReader.city(ipAddress);
      if (response == null) {
        LOG.debug("CityResponse is null");
        return null;
      }
      if (LOG.isTraceEnabled()) {
        LOG.trace(response.toJson());
      }

      Continent continent = response.continent();
      if (continent != null) {
        geoIP.setContinent(continent.name());
      }

      Country country = response.country();
      if (country != null) {
        geoIP.setCountryISOCode(country.isoCode());
        geoIP.setCountry(country.name());
      }

      Subdivision subdivision = response.mostSpecificSubdivision();
      if (subdivision != null) {
        geoIP.setStateISOCode(subdivision.isoCode());
        geoIP.setState(subdivision.name());
      } else if (response.leastSpecificSubdivision() != null) {
        geoIP.setStateISOCode(response.leastSpecificSubdivision().isoCode());
        geoIP.setState(response.leastSpecificSubdivision().name());
      }

      City city = response.city();
      if (city != null) {
        geoIP.setCity(city.name());
      }

      Postal postal = response.postal();
      if (postal != null) {
        geoIP.setPostalCode(postal.code());
      }

      Location location = response.location();
      if (location != null) {
        geoIP.setTimezone(location.timeZone());
        geoIP.setLatitude(location.latitude());
        geoIP.setLongitude(location.longitude());
      }
      return geoIP;
    } catch (Exception e) {
      LOG.debug("Could not geo-ip: " + e.getMessage());
    }
    return null;
  }

  public static String getCityStateCountryLocation(String ip) {
    return getCityStateCountryLocation(ip, "unknown");
  }

  public static String getCityStateCountryLocation(String ip, String unknownText) {
    GeoIP geoIP = getLocation(ip);
    if (geoIP == null) {
      return unknownText;
    }
    StringBuilder sb = new StringBuilder();
    // City
    if (StringUtils.isNotBlank(geoIP.getCity())) {
      sb.append(geoIP.getCity());
    }
    // State
    if (StringUtils.isNotBlank(geoIP.getState())) {
      if (sb.length() == 0) {
        sb.append(geoIP.getState());
      } else if (!geoIP.getState().equals(geoIP.getCity())) {
        sb.append(", ").append(geoIP.getState());
      }
    }
    // Country
    if (StringUtils.isNotBlank(geoIP.getCountry()) && !"United States".equals(geoIP.getCountry())) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(geoIP.getCountry());
    }
    return sb.toString();
  }

  public static String getCityStateLocation(String ip, String unknownText) {
    GeoIP geoIP = getLocation(ip);
    if (geoIP == null) {
      return unknownText;
    }
    StringBuilder sb = new StringBuilder();
    // City
    if (StringUtils.isNotBlank(geoIP.getCity())) {
      sb.append(geoIP.getCity());
    }
    // State
    if (StringUtils.isNotBlank(geoIP.getState())) {
      if (sb.length() == 0) {
        sb.append(geoIP.getState());
      } else if (!geoIP.getState().equals(geoIP.getCity())) {
        sb.append(", ").append(geoIP.getState());
      }
    }
    return sb.toString();
  }

  public static String getCountryLocation(String ip, String unknownText) {
    GeoIP geoIP = getLocation(ip);
    if (geoIP == null) {
      return unknownText;
    }
    if (StringUtils.isNotBlank(geoIP.getCountry())) {
      return geoIP.getCountry();
    }
    return unknownText;
  }

  public static String getCountryCode(String ip) {
    if (StringUtils.isBlank(ip) || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
      LOG.debug("Skipping for localhost");
      return null;
    }
    if (countryReader == null) {
      return null;
    }
    try {
      LOG.debug("Checking IP: " + ip);
      InetAddress ipAddress = InetAddress.getByName(ip);
      CountryResponse response = countryReader.country(ipAddress);
      if (response == null) {
        LOG.debug("CountryResponse is null");
        return null;
      }
      if (LOG.isTraceEnabled()) {
        LOG.trace(response.toJson());
      }
      Country country = response.country();
      if (country != null) {
        return country.isoCode();
      }
    } catch (Exception e) {
      LOG.warn("Country from IP error: " + e.getMessage());
    }
    return null;
  }
}
