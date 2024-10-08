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

package com.simisinc.platform.application.items;

import com.simisinc.platform.domain.model.items.CollectionRelationship;
import com.simisinc.platform.infrastructure.persistence.items.CollectionRelationshipRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads a collection's relationship list
 *
 * @author matt rajkowski
 * @created 7/27/18 11:43 AM
 */
public class LoadCollectionRelationshipListCommand {

  private static Log LOG = LogFactory.getLog(LoadCollectionRelationshipListCommand.class);

  public static List<CollectionRelationship> findAllByCollectionId(long collectionId) {
    List<CollectionRelationship> relationshipList = new ArrayList<>();
    List<CollectionRelationship> parentRelationshipList = CollectionRelationshipRepository.findAllParentsByCollectionId(collectionId);
    List<CollectionRelationship> selfRelationshipList = CollectionRelationshipRepository.findAllSelfByCollectionId(collectionId);
    List<CollectionRelationship> childRelationshipList = CollectionRelationshipRepository.findAllChildrenByCollectionId(collectionId);
    if (parentRelationshipList != null) {
      relationshipList.addAll(parentRelationshipList);
    }
    if (selfRelationshipList != null) {
      relationshipList.addAll(selfRelationshipList);
    }
    if (childRelationshipList != null) {
      relationshipList.addAll(childRelationshipList);
    }
    return relationshipList;
  }

}
