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

package com.simisinc.platform.application.datasets;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;

import com.fasterxml.jackson.databind.JsonNode;
import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.persistence.items.ItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemSpecification;

/**
 * Reads in dataset rows from a GeoJSON dataset file
 *
 * @author matt rajkowski
 * @created 2/3/19 2:22 PM
 */
public class LoadGeoJsonFeedCommand {

  private static Log LOG = LogFactory.getLog(LoadGeoJsonFeedCommand.class);

  public static List<String[]> loadRows(Dataset dataset, int maxRowCountToReturn) throws DataException {
    File file = DatasetFileCommand.getFile(dataset);
    if (file == null) {
      throw new DataException("Dataset file not found");
    }
    return loadRows(dataset, file, maxRowCountToReturn);
  }

  public static List<GeoJsonObject> loadRecords(Dataset dataset) throws DataException {
    File file = DatasetFileCommand.getFile(dataset);
    if (file == null) {
      return null;
    }
    return loadRecords(dataset, file);
  }

  private static List<String[]> loadRows(Dataset dataset, File file, int maxRowCountToReturn) throws DataException {
    List<String[]> rows = new ArrayList<>();
    if (file == null) {
      return rows;
    }
    try {
      JsonNode config = JsonCommand.fromFile(file);
      Iterator<JsonNode> features = config.get("features").elements();
      for (int i = 0; i < maxRowCountToReturn; i++) {
        if (!features.hasNext()) {
          return rows;
        }
        JsonNode thisFeature = features.next();
        LOG.debug("Got next feature...");
        JsonNode node = thisFeature.get("attributes");
        List<String> fields = new ArrayList<>();
        for (String column : dataset.getColumnNamesList()) {
          String nodeValue = "";
          if (node.has(column)) {
            nodeValue = node.get(column).asText();
          }
          if (nodeValue == null || nodeValue.equalsIgnoreCase("null")) {
            nodeValue = "";
          }
          //nodeValue = StringUtils.abbreviate(nodeValue, 30);
          fields.add(nodeValue);
        }
        rows.add(fields.toArray(new String[0]));
      }
    } catch (Exception e) {
      LOG.error("GeoJson Error: " + e.getMessage());
      throw new DataException("File could not be read");
    }
    return rows;
  }

  private static List<GeoJsonObject> loadRecords(Dataset dataset, File file) throws DataException {
    List<GeoJsonObject> recordList = new ArrayList<>();
    if (file == null) {
      return recordList;
    }
    try {
      JsonNode json = JsonCommand.fromFile(file);
      processFeatures(json, recordList, dataset.getColumnNamesList(), null);
    } catch (Exception e) {
      LOG.error("GeoJson Error: " + e.getMessage());
      throw new DataException("File could not be read");
    }
    return recordList;
  }

  public static List<GeoJsonObject> loadRecords(Collection collection) throws DataException {
    ItemSpecification specification = new ItemSpecification();
    specification.setCollectionId(collection.getId());
    specification.setHasGeoJSON(true);
    List<Item> itemList = ItemRepository.findAll(specification, null);
    List<GeoJsonObject> recordList = new ArrayList<>();
    if (itemList == null || itemList.isEmpty()) {
      return recordList;
    }
    for (Item item : itemList) {
      try {
        JsonNode json = JsonCommand.fromString(item.getGeoJSON());
        processFeatures(json, recordList, null, item);
      } catch (Exception e) {
        LOG.info("Invalid geoJSON found for item: " + item.getUniqueId());
      }
    }
    return recordList;

  }

  private static void processFeatures(JsonNode json, List<GeoJsonObject> recordList, List<String> columnNames, Item item) {

    Iterator<JsonNode> features = json.get("features").elements();

    while (features.hasNext()) {

      JsonNode thisFeature = features.next();
      Feature record = new Feature();

      // Give the feature a name
      if (item != null) {
        record.setProperty("NAME", item.getName());
        record.setProperty("OBJECTID", item.getUniqueId());
        if (item.hasGeoPoint()) {
          record.setProperty("latitude", item.getLatitude());
          record.setProperty("longitude", item.getLongitude());
        }
      }

      // Determine the attributes or properties of the feature
      if (columnNames != null) {
        // Check the standard properties field
        JsonNode attributes = thisFeature.get("properties");
        if (!thisFeature.has("properties")) {
          // Check ArcGIS/esri attributes
          attributes = thisFeature.get("attributes");
        }
        for (String column : columnNames) {
          String nodeValue = "";
          if (attributes.has(column)) {
            nodeValue = attributes.get(column).asText();
          }
          if (nodeValue == null || nodeValue.equalsIgnoreCase("null")) {
            nodeValue = "";
          }
          record.setProperty(column, nodeValue);
        }
      }

      // Determine the geometry
      if (thisFeature.has("geometry")) {
        JsonNode geometry = thisFeature.get("geometry");
        JsonNode coordinates = null;
        if (geometry.has("coordinates")) {
          coordinates = geometry.get("coordinates");
        } else if (geometry.has("rings")) {
          // Check ArcGIS/esri attributes
          coordinates = geometry.get("rings");
        }
        if (coordinates == null) {
          continue;
        }
        if (coordinates.isContainerNode()) {
          List<LngLatAlt> points = new ArrayList<>();
          Iterator<JsonNode> polygonIterator = coordinates.elements().next().elements();
          while (polygonIterator.hasNext()) {
            Iterator<JsonNode> pointsIterator = polygonIterator.next().elements();
            LngLatAlt point = new LngLatAlt(pointsIterator.next().asDouble(), pointsIterator.next().asDouble());
            points.add(point);
          }
          Polygon polygon = new Polygon(points);
          record.setGeometry(polygon);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Coordinates: " + points.size() + " and " + ((Polygon) record.getGeometry()).getExteriorRing().size());
          }
        }
      }

      // Determine the centroid
      if (thisFeature.has("centroid")) {
        JsonNode centroid = thisFeature.get("centroid");
        if (centroid.has("x") && centroid.has("y")) {
          record.setProperty("longitude", centroid.get("x").toString());
          record.setProperty("latitude", centroid.get("y").toString());
        }
      } else {
        // Just use the first value
        LngLatAlt coordinates = ((Polygon) record.getGeometry()).getExteriorRing().get(0);
        record.setProperty("longitude", String.valueOf(coordinates.getLongitude()));
        record.setProperty("latitude", String.valueOf(coordinates.getLatitude()));
      }

      recordList.add(record);
    }
  }
}
