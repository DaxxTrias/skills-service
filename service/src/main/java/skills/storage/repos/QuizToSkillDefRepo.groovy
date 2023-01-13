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
package skills.storage.repos

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.lang.Nullable
import skills.storage.model.QuizToSkillDef

interface QuizToSkillDefRepo extends JpaRepository<QuizToSkillDef, Long> {

    static interface QuizNameAndId {
        Integer getSkillRefId()
        String getQuizName()
        String getQuizId()
    }

    static interface ProjectIdAndSkillId {
        String getProjectId()
        String getSkillId()
    }

    @Nullable
    @Query('''select q.quizId as quizId, q.name as quizName, qToS.skillRefId as skillRefId
            from QuizToSkillDef qToS, QuizDef q 
            where qToS.skillRefId = ?1
                and q.id = qToS.quizRefId''')
    QuizNameAndId getQuizIdBySkillIdRef(Integer skillIdRef)

    @Nullable
    @Query('''select skill.skillId as skillId, skill.projectId as projectId
            from QuizToSkillDef qToS, SkillDef skill 
            where qToS.quizRefId = ?1
                and skill.id = qToS.skillRefId''')
    List<ProjectIdAndSkillId> getSkillsForQuiz(Integer quizRefId)

    @Nullable
    @Query('''select q.quizId as quizId, q.name as quizName, qToS.skillRefId as skillRefId
            from QuizToSkillDef qToS, QuizDef q 
            where qToS.skillRefId in ?1
                and q.id = qToS.quizRefId''')
    List<QuizNameAndId> getQuizInfoSkillIdRef(List<Integer> skillIdRef)

    void deleteBySkillRefId(Integer skillRefId)
}

