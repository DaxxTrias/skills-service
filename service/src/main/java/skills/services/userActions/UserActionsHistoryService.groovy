/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.services.userActions

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.FilterProvider
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.SkillException
import skills.controller.result.model.DashboardUserActionRes
import skills.controller.result.model.TableResult
import skills.services.inception.InceptionProjectService
import skills.storage.model.UserActionsHistory
import skills.storage.repos.UserActionsHistoryRepo

@Service
@Slf4j
class UserActionsHistoryService {

    @Autowired
    UserActionsHistoryRepo userActionsHistoryRepo

    @Autowired
    UserInfoService userInfoService

    @JsonFilter("DynamicFilter")
    class DynamicFilterMixIn {
    }

    ObjectMapper mapper
    @PostConstruct
    void init() {
        mapper = new ObjectMapper().addMixIn(Object.class, DynamicFilterMixIn.class)
        SimpleBeanPropertyFilter simpleBeanPropertyFilter = SimpleBeanPropertyFilter.serializeAllExcept("clientSecret", "password")
        FilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("DynamicFilter", (SimpleBeanPropertyFilter)simpleBeanPropertyFilter);
        mapper.setFilterProvider(filterProvider)
    }

    @Transactional
    void saveUserAction(UserActionInfo userActionInfo){
        if (userActionInfo.projectId == InceptionProjectService.inceptionProjectId) {
            return
        }
        String actionAttributesAsStr = userActionInfo.actionAttributes ? mapper.writeValueAsString(userActionInfo.actionAttributes) : null
        UserActionsHistory userActionsHistory = new UserActionsHistory(
                action: userActionInfo.action,
                item: userActionInfo.item,
                itemId: userActionInfo.itemId,
                itemRefId: userActionInfo.itemRefId,
                userId: userInfoService.currentUserId,
                projectId: userActionInfo.projectId,
                quizId: userActionInfo.quizId,
                actionAttributes: actionAttributesAsStr,
        )
        userActionsHistoryRepo.save(userActionsHistory)
    }

    @Transactional
    TableResult getUsersActions(PageRequest pageRequest,
                                String projectIdFilter,
                                DashboardItem itemFilter,
                                String userFilter,
                                String quizFilter,
                                String itemIdFilter,
                                DashboardAction actionFilter) {
        Page<UserActionsHistoryRepo.UserActionsPreview> userActionsPreviewFromDB = userActionsHistoryRepo.getActions(
                projectIdFilter, itemFilter, userFilter, quizFilter, itemIdFilter, actionFilter, pageRequest)
        Long totalRows = userActionsPreviewFromDB.getTotalElements()
        List<DashboardUserActionRes> actionResList = userActionsPreviewFromDB.getContent().collect {
            new DashboardUserActionRes(
                    id: it.id,
                    action: it.action,
                    item: it.item,
                    itemId: it.itemId,
                    itemRefId: it.itemRefId,
                    userId: it.userId,
                    userIdForDisplay: it.userIdForDisplay,
                    projectId: it.projectId,
                    quizId: it.quizId,
                    created: it.created,
            )
        }

        return new TableResult(
                count: totalRows,
                totalCount: totalRows,
                data: actionResList
        )
    }

    Map getActionAttributes(Long id) {
        Optional<UserActionsHistory> optional = userActionsHistoryRepo.findById(id)
        if (optional.empty) {
            throw new SkillException("Failed to locate UserActionsHistory by id [${id}]");
        }
        UserActionsHistory userActionsHistory = optional.get()
        userActionsHistory.actionAttributes

        Map result = mapper.readValue(userActionsHistory.actionAttributes, Map.class)
        return result
    }
}
