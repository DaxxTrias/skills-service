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

import groovy.util.logging.Slf4j
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService

import static skills.intTests.utils.SkillsFactory.createProject
import static skills.intTests.utils.SkillsFactory.createSubject

@Slf4j
class UserCommunityAuthSpecs extends DefaultIntSpec {
    String ultimateRoot = 'jh@dojo.com'
    SkillsService rootSkillsService

    def setup() {
        rootSkillsService = createService(ultimateRoot, 'aaaaaaaa')
        if (!rootSkillsService.isRoot()) {
            rootSkillsService.grantRoot()
        }
    }

    def "cannot access a project with user community protection enabled when the user does not belong to the user community"() {

        when:
        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon']);

        def proj = createProject(1, true)
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
        Map badge = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(proj.projectId, badge.badgeId, skill.skillId)

        def nonUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService nonUserCommunityUser = createService(nonUserCommunityUserId)

        then:
        validateForbidden { nonUserCommunityUser.apiGetUserLevelForProject(proj.projectId, null) }
        validateForbidden { nonUserCommunityUser.lookupMyProjectName(proj.projectId) }
        validateForbidden { nonUserCommunityUser.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0") }
        validateForbidden { nonUserCommunityUser.contactProjectOwner(proj.projectId, "a message") }
        validateForbidden { nonUserCommunityUser.getSubjectDescriptions(proj.projectId, subj.subjectId) }
        validateForbidden { nonUserCommunityUser.getBadgeDescriptions(proj.projectId, badge.badgeId) }
        validateForbidden { nonUserCommunityUser.addSkill([projectId: proj.projectId, skillId: skill.skillId]) }
        validateForbidden { nonUserCommunityUser.getSkillSummary(null, proj.projectId, subj.subjectId) }
        validateForbidden { nonUserCommunityUser.documentVisitedSkillId(proj.projectId, skill.skillId) }
        validateForbidden { nonUserCommunityUser.getSkillsSummaryForCurrentUser(proj.projectId) }
        validateForbidden { nonUserCommunityUser.addMyProject(proj.projectId) }
        validateForbidden { nonUserCommunityUser.moveMyProject(proj.projectId, 1) }
        validateForbidden { nonUserCommunityUser.removeMyProject(proj.projectId) }
        validateForbidden { nonUserCommunityUser.getSingleSkillSummaryForCurrentUser(proj.projectId, skill.skillId) }
        validateForbidden { nonUserCommunityUser.getSubjectSummaryForCurrentUser(proj.projectId, subj.subjectId) }
        validateForbidden { nonUserCommunityUser.getBadgesSummary(null, proj.projectId) }
        validateForbidden { nonUserCommunityUser.getBadgeSummary(null, proj.projectId, badge.badgeId) }
        validateForbidden { nonUserCommunityUser.getUsersPerLevel(proj.projectId) }
        validateForbidden { nonUserCommunityUser.getRank(null, proj.projectId) }
        validateForbidden { nonUserCommunityUser.getRank(null, proj.projectId, subj.subjectId)}
        validateForbidden { nonUserCommunityUser.getLeaderboard(null, proj.projectId)}
        validateForbidden { nonUserCommunityUser.getLeaderboard(null, proj.projectId, subj.subjectId)}
        validateForbidden { nonUserCommunityUser.getRankDistribution(null, proj.projectId)}
        validateForbidden { nonUserCommunityUser.getRankDistribution(null, proj.projectId, subj.subjectId)}
        validateForbidden { nonUserCommunityUser.getPointHistory(null, proj.projectId)}
        validateForbidden { nonUserCommunityUser.getPointHistory(null, proj.projectId, subj.subjectId)}
    }

    def "can access a project with user community protection enabled when the user does belong to the user community"() {
        when:
        String userCommunityUserId =  skillsService.userName
        rootSkillsService.saveUserTag(userCommunityUserId, 'dragons', ['DivineDragon']);

        def proj = createProject(1, true)
        def subj = createSubject(1, 1)
        def skill = SkillsFactory.createSkill(1, 1)
        skillsService.createProjectAndSubjectAndSkills(proj, subj, [skill])
        Map badge = SkillsFactory.createBadge(1, 1)
        skillsService.createBadge(badge)
        skillsService.assignSkillToBadge(proj.projectId, badge.badgeId, skill.skillId)

        def otherUserCommunityUserId = getRandomUsers(1, true, ['skills@skills.org', DEFAULT_ROOT_USER_ID])[0]
        SkillsService otherUserCommunityUser = createService(otherUserCommunityUserId)
        rootSkillsService.saveUserTag(otherUserCommunityUserId, 'dragons', ['DivineDragon']);

        then:
        !validateForbidden { otherUserCommunityUser.apiGetUserLevelForProject(proj.projectId, null) }
        !validateForbidden { otherUserCommunityUser.lookupMyProjectName(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.reportClientVersion(proj.projectId, "@skilltree/skills-client-fake-1.0.0") }
        !validateForbidden { otherUserCommunityUser.contactProjectOwner(proj.projectId, "a message") }
        !validateForbidden { otherUserCommunityUser.getSubjectDescriptions(proj.projectId, subj.subjectId) }
        !validateForbidden { otherUserCommunityUser.getBadgeDescriptions(proj.projectId, badge.badgeId) }
        !validateForbidden { otherUserCommunityUser.addSkill([projectId: proj.projectId, skillId: skill.skillId]) }
        !validateForbidden { otherUserCommunityUser.getSkillSummary(null, proj.projectId, subj.subjectId) }
        !validateForbidden { otherUserCommunityUser.documentVisitedSkillId(proj.projectId, skill.skillId) }
        !validateForbidden { otherUserCommunityUser.getSkillsSummaryForCurrentUser(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.addMyProject(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.moveMyProject(proj.projectId, 1) }
        !validateForbidden { otherUserCommunityUser.removeMyProject(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.getSingleSkillSummaryForCurrentUser(proj.projectId, skill.skillId) }
        !validateForbidden { otherUserCommunityUser.getSubjectSummaryForCurrentUser(proj.projectId, subj.subjectId) }
        !validateForbidden { otherUserCommunityUser.getBadgesSummary(null, proj.projectId) }
        !validateForbidden { otherUserCommunityUser.getBadgeSummary(null, proj.projectId, badge.badgeId) }
        !validateForbidden { otherUserCommunityUser.getUsersPerLevel(proj.projectId) }
        !validateForbidden { otherUserCommunityUser.getRank(null, proj.projectId) }
        !validateForbidden { otherUserCommunityUser.getRank(null, proj.projectId, subj.subjectId)}
        !validateForbidden { otherUserCommunityUser.getLeaderboard(null, proj.projectId)}
        !validateForbidden { otherUserCommunityUser.getLeaderboard(null, proj.projectId, subj.subjectId)}
        !validateForbidden { otherUserCommunityUser.getRankDistribution(null, proj.projectId)}
        !validateForbidden { otherUserCommunityUser.getRankDistribution(null, proj.projectId, subj.subjectId)}
        !validateForbidden { otherUserCommunityUser.getPointHistory(null, proj.projectId)}
        !validateForbidden { otherUserCommunityUser.getPointHistory(null, proj.projectId, subj.subjectId)}
    }

    private boolean validateForbidden(Closure c) {
        try {
            c.call()
            return false
        } catch (SkillsClientException skillsClientException) {
            return skillsClientException.httpStatus == org.springframework.http.HttpStatus.FORBIDDEN
        }
        return false
    }

}

