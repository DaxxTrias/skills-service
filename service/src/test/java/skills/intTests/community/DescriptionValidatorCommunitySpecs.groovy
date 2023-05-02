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
package skills.intTests.community


import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsService
import skills.services.settings.Settings
import skills.storage.model.auth.RoleName

import static skills.intTests.utils.SkillsFactory.createProject

class DescriptionValidatorCommunitySpecs extends DefaultIntSpec {

    String notValidDefault = "has jabberwocky"
    String notValidProtectedCommunity = "has divinedragon"

    def "description validator for community without projectId"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        when:
        def defaultResValid = skillsService.checkCustomDescriptionValidation(notValidProtectedCommunity)
        def defaultResInValid = skillsService.checkCustomDescriptionValidation(notValidDefault)

        then:
        defaultResValid.body.valid
        !defaultResInValid.body.valid
        defaultResInValid.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "description validator for community with projectId"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2)
        pristineDragonsUser.createProject(p2)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, p1.projectId)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, p1.projectId)

        def communityValidP2 = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, p2.projectId)
        def communityInvalidValidP2 = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, p2.projectId)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == "May not contain divinedragon word"

        communityValidP2.body.valid
        !communityInvalidValidP2.body.valid
        communityInvalidValidP2.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "description validator for community with useProtectedCommunityValidator"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, true)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, true)

        def defaultResValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, null, false)
        def defaultResInvalid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, null, false)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == "May not contain divinedragon word"

        defaultResValid.body.valid
        !defaultResInvalid.body.valid
        defaultResInvalid.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "description validator for community with projectId overrides useProtectedCommunityValidator"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2)
        pristineDragonsUser.createProject(p2)

        when:
        def communityValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, p1.projectId, true)
        def communityInvalidValid = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, p1.projectId, true)

        def communityValidP2 = pristineDragonsUser.checkCustomDescriptionValidation(notValidProtectedCommunity, p2.projectId, true)
        def communityInvalidValidP2 = pristineDragonsUser.checkCustomDescriptionValidation(notValidDefault, p2.projectId,true)
        then:
        communityValid.body.valid
        !communityInvalidValid.body.valid
        communityInvalidValid.body.msg == "May not contain divinedragon word"

        communityValidP2.body.valid
        !communityInvalidValidP2.body.valid
        communityInvalidValidP2.body.msg == "paragraphs may not contain jabberwocky"
    }

    def "only community member can call description validator for community with useProtectedCommunityValidator"() {
        when:
        skillsService.checkCustomDescriptionValidation(notValidDefault, null, true)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User [${skillsService.userName}] is not allowed to validate using user community validation")
    }

    def "only community member can call description validator for community with projectId that belongs to that community"() {
        List<String> users = getRandomUsers(2)

        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        when:
        skillsService.checkCustomDescriptionValidation(notValidDefault, p1.projectId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User [${skillsService.userName}] is not allowed to validate using user community validation")
    }

}
