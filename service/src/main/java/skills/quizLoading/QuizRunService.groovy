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
package skills.quizLoading

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.QuizValidator
import skills.controller.exceptions.SkillQuizException
import skills.quizLoading.model.*
import skills.services.events.SkillEventResult
import skills.services.events.SkillEventsService
import skills.services.quiz.QuizQuestionType
import skills.storage.model.*
import skills.storage.repos.*

@Service
@Slf4j
class QuizRunService {

    @Autowired
    QuizDefWithDescRepo quizDefWithDescRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizQuestionDefRepo quizQuestionRepo

    @Autowired
    QuizAnswerDefRepo quizAnswerRepo

    @Autowired
    UserQuizAttemptRepo quizAttemptRepo

    @Autowired
    QuizSettingsRepo quizSettingsRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Autowired
    SkillEventsService skillEventsService

    @Autowired
    UserQuizQuestionAttemptRepo quizQuestionAttemptRepo

    @Autowired
    UserQuizAnswerAttemptRepo quizAttemptAnswerRepo

    @Autowired
    UserInfoService userInfoService

    @Transactional
    QuizInfo loadQuizInfo(String quizId) {
        UserAttrs currentUserAttrs = userInfoService.getCurrentUserAttrs()
        return this.loadQuizInfo(currentUserAttrs.userId, quizId)
    }

    @Transactional
    QuizInfo loadQuizInfo(String userId, String quizId) {
        QuizDefWithDescription updatedDef = quizDefWithDescRepo.findByQuizIdIgnoreCase(quizId)
        if (!updatedDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }

        List<QuizQuestionInfo> questions = loadQuizQuestionInfo(quizId)

        UserQuizAttemptRepo.UserQuizAttemptStats userAttemptsStats =
                quizAttemptRepo.getUserAttemptsStats(userId, updatedDef.id,
                        UserQuizAttempt.QuizAttemptStatus.INPROGRESS, UserQuizAttempt.QuizAttemptStatus.PASSED)

        return new QuizInfo(
                name: updatedDef.name,
                description: updatedDef.description,
                questions: questions,
                quizType: updatedDef.getType().toString(),
                isAttemptAlreadyInProgress: userAttemptsStats?.getIsAttemptAlreadyInProgress() ?: false,
                userNumPreviousQuizAttempts: userAttemptsStats?.getUserNumPreviousQuizAttempts() ?: 0,
                userQuizPassed: userAttemptsStats?.getUserQuizPassed() ?: false,
                userLastQuizAttemptDate: userAttemptsStats?.getUserLastQuizAttemptCompleted() ?: null,
        )
    }

    private List<QuizQuestionInfo> loadQuizQuestionInfo(String quizId) {
        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)
        List<QuizAnswerDef> dbAnswersDef = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        Map<Integer, List<QuizAnswerDef>> byQuizId = dbAnswersDef.groupBy { it.questionRefId }

        List<QuizQuestionInfo> questions = dbQuestionDefs.collect {
            List<QuizAnswerDef> quizAnswerDefs = byQuizId[it.id]
            new QuizQuestionInfo(
                    id: it.id,
                    question: it.question,
                    questionType: it.type.toString(),
                    canSelectMoreThanOne: quizAnswerDefs.count({ Boolean.valueOf(it.isCorrectAnswer) }) > 1,
                    answerOptions: quizAnswerDefs.collect {
                        new QuizAnswerOptionsInfo(
                                id: it.id,
                                answerOption: it.answer
                        )
                    }
            )
        }
        return questions
    }

    @Transactional
    QuizAttemptStartResult startQuizAttempt(String quizId) {
        UserAttrs currentUserAttrs = userInfoService.getCurrentUserAttrs()
        return this.startQuizAttempt(currentUserAttrs.userId, quizId)
    }

    @Transactional
    QuizAttemptStartResult startQuizAttempt(String userId, String quizId) {
        UserQuizAttempt inProgressAttempt = quizAttemptRepo.getByUserIdAndQuizIdAndState(userId, quizId, UserQuizAttempt.QuizAttemptStatus.INPROGRESS)
        if (inProgressAttempt) {
            List<UserQuizAnswerAttemptRepo.AnswerIdAndAnswerText> alreadySelected = quizAttemptAnswerRepo.getSelectedAnswerIdsAndText(inProgressAttempt.id)

            List<Integer> selectedAnswerIds = alreadySelected?.findAll({!it.getAnswerText()}).collect { it.getAnswerId()}
            List<QuizAttemptStartResult.AnswerIdAndEnteredText> enteredText = alreadySelected?.findAll({it.getAnswerText()}).collect {
                new QuizAttemptStartResult.AnswerIdAndEnteredText(answerId: it.getAnswerId(), answerText: it.getAnswerText())
            }

            log.info("Continued existing quiz attempt {}", inProgressAttempt)
            return new QuizAttemptStartResult(
                    id: inProgressAttempt.id,
                    inProgressAlready: true,
                    selectedAnswerIds: selectedAnswerIds ?: [],
                    enteredText: enteredText,
            )
        }

        QuizDef quizDef = getQuizDef(quizId)
        validateQuizAttempts(quizDef, userId, quizId)

        UserQuizAttempt userQuizAttempt = new UserQuizAttempt(
                userId: userId,
                quizDefinitionRefId: quizDef.id,
                status: UserQuizAttempt.QuizAttemptStatus.INPROGRESS,
                started: new Date())
        UserQuizAttempt savedAttempt = quizAttemptRepo.saveAndFlush(userQuizAttempt)
        log.info("Started new quiz attempt {}", savedAttempt)
        return new QuizAttemptStartResult(id: savedAttempt.id)
    }

    @Profile
    private void validateQuizAttempts(QuizDef quizDef, String userId, String quizId) {
        UserQuizAttemptRepo.UserQuizAttemptStats userAttemptsStats = quizAttemptRepo.getUserAttemptsStats(userId, quizDef.id,
                UserQuizAttempt.QuizAttemptStatus.INPROGRESS, UserQuizAttempt.QuizAttemptStatus.PASSED)
        Integer numCurrentAttempts = userAttemptsStats?.getUserNumPreviousQuizAttempts() ?: 0
        if (quizDef.type == QuizDefParent.QuizType.Survey) {
            if (numCurrentAttempts > 0) {
                throw new SkillQuizException("User [${userId}] has already taken this survey", quizId, ErrorCode.BadParam)
            }
        } else {
            if (userAttemptsStats?.getUserQuizPassed()) {
                throw new SkillQuizException("User [${userId}] already took and passed this quiz.", quizId, ErrorCode.UserQuizAttemptsExhausted)
            }

            QuizSetting quizSetting = quizSettingsRepo.findBySettingAndQuizRefId(QuizSettings.MaxNumAttempts.setting, quizDef.id)
            if (quizSetting) {
                int numConfiguredAttempts = Integer.valueOf(quizSetting.value)
                // anything 0 or below is considered to be unlimited attempts
                if (numConfiguredAttempts > 0 && numCurrentAttempts >= numConfiguredAttempts) {
                    throw new SkillQuizException("User [${userId}] exhausted [${numConfiguredAttempts}] available attempts for this quiz.", quizId, ErrorCode.UserQuizAttemptsExhausted)
                }
            }
        }
    }

    @Transactional
    void reportQuestionAnswer(String quizId, Integer attemptId, Integer answerDefId, QuizReportAnswerReq quizReportAnswerReq) {
        UserAttrs currentUserAttrs = userInfoService.getCurrentUserAttrs()
        this.reportQuestionAnswer(currentUserAttrs.userId, quizId, attemptId, answerDefId, quizReportAnswerReq)
    }

    @Transactional
    void reportQuestionAnswer(String userId, String quizId, Integer quizAttemptId, Integer answerDefId, QuizReportAnswerReq quizReportAnswerReq) {
        if (!quizAttemptRepo.existsByUserIdAndIdAndQuizId(userId, quizAttemptId, quizId)) {
            throw new SkillQuizException("Provided attempt id [${quizAttemptId}] does not exist for [${userId}] user and [${quizId}] quiz", ErrorCode.BadParam)
        }

        QuizAnswerDefRepo.AnswerDefPartialInfo answerDefPartialInfo = quizAnswerRepo.getPartialDefByAnswerDefId(answerDefId)
        if (!answerDefPartialInfo) {
            throw new SkillQuizException("Provided answer id [${answerDefId}] does not exist", ErrorCode.BadParam)
        }
        if (answerDefPartialInfo.getQuizId() != quizId) {
            throw new SkillQuizException("Supplied quizId of [${quizId}] does not match answer's quiz id  of [${answerDefPartialInfo.getQuizId()}]", ErrorCode.BadParam)
        }

        if (answerDefPartialInfo.getQuestionType() == QuizQuestionType.TextInput) {
            QuizValidator.isNotBlank(quizReportAnswerReq.getAnswerText(), "answerText", quizId)
            handleReportingTextInputQuestion(userId, quizAttemptId, answerDefId, quizReportAnswerReq)
        } else {
            handleReportingAChoiceBasedQuestion(userId, quizAttemptId, answerDefId, quizReportAnswerReq, answerDefPartialInfo)
        }
    }

    private void handleReportingTextInputQuestion(String userId, Integer quizAttemptId, Integer answerDefId, QuizReportAnswerReq quizReportAnswerReq) {
        UserQuizAnswerAttempt existingAnswerAttempt = quizAttemptAnswerRepo.findByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(quizAttemptId, answerDefId)
        if (existingAnswerAttempt) {
            existingAnswerAttempt.answer = quizReportAnswerReq.getAnswerText()
            quizAttemptAnswerRepo.save(existingAnswerAttempt)
        } else {
            UserQuizAnswerAttempt newAnswerAttempt = new UserQuizAnswerAttempt(
                    userQuizAttemptRefId: quizAttemptId,
                    quizAnswerDefinitionRefId: answerDefId,
                    userId: userId,
                    status: UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT,
                    answer: quizReportAnswerReq.getAnswerText(),
            )
            quizAttemptAnswerRepo.save(newAnswerAttempt)
        }
    }

    private void handleReportingAChoiceBasedQuestion(String userId, Integer attemptId, Integer answerDefId, QuizReportAnswerReq quizReportAnswerReq, QuizAnswerDefRepo.AnswerDefPartialInfo answerDefPartialInfo) {
        if (!quizReportAnswerReq.isSelected) {
            quizAttemptAnswerRepo.deleteByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(attemptId, answerDefId)
        } else {
            boolean isCorrectChoice = Boolean.valueOf(answerDefPartialInfo.getIsCorrectAnswer())
            boolean answerAttemptAlreadyDocumented = quizAttemptAnswerRepo.existsByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(attemptId, answerDefId)
            if (!answerAttemptAlreadyDocumented) {
                UserQuizAnswerAttempt answerAttempt = new UserQuizAnswerAttempt(
                        userQuizAttemptRefId: attemptId,
                        quizAnswerDefinitionRefId: answerDefId,
                        userId: userId,
                        status: isCorrectChoice ? UserQuizAnswerAttempt.QuizAnswerStatus.CORRECT : UserQuizAnswerAttempt.QuizAnswerStatus.WRONG,
                )
                quizAttemptAnswerRepo.save(answerAttempt)
            } else {
                log.warn("Answer was already persisted for user [{}] for answerDefId of [{}]", userId, answerDefId)
            }

            List<QuizAnswerDefRepo.AnswerIdAndCorrectness> questions = quizAnswerRepo.getAnswerIdsAndCorrectnessIndicator(answerDefPartialInfo.getQuestionRefId())
            assert questions
            boolean isMultipleChoice = questions.count { Boolean.valueOf(it.getIsCorrectAnswer()) } > 1
            if (!isMultipleChoice) {
                List<Integer> toRemove = questions.findAll { it.getAnswerRefId() != answerDefId }.collect { it.getAnswerRefId() }
                toRemove.each {
                    quizAttemptAnswerRepo.deleteByUserQuizAttemptRefIdAndQuizAnswerDefinitionRefId(attemptId, it)
                }
            }
        }
    }

    @Transactional
    QuizGradedResult completeQuizAttempt(String quizId, Integer quizAttemptId) {
        UserAttrs currentUserAttrs = userInfoService.getCurrentUserAttrs()
        return this.completeQuizAttempt(currentUserAttrs.userId, quizId, quizAttemptId)
    }

    @Transactional
    QuizGradedResult completeQuizAttempt(String userId, String quizId, Integer quizAttemptId) {
        QuizDef quizDef = getQuizDef(quizId)
        Optional<UserQuizAttempt> optionalUserQuizAttempt = quizAttemptRepo.findById(quizAttemptId)
        if (!optionalUserQuizAttempt.isPresent()) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] does not exist", ErrorCode.BadParam)
        }
        UserQuizAttempt userQuizAttempt = optionalUserQuizAttempt.get()
        if (userQuizAttempt.quizDefinitionRefId != quizDef.id) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] is not for [${quizId}] quiz", ErrorCode.BadParam)
        }
        if (userQuizAttempt.userId != userId) {
            throw new SkillQuizException("Provided quiz attempt id [${quizAttemptId}] is not for [${userId}] user", ErrorCode.BadParam)
        }

        List<QuizQuestionDef> dbQuestionDefs = quizQuestionRepo.findAllByQuizIdIgnoreCase(quizId)
        List<QuizAnswerDef> dbAnswersDefs = quizAnswerRepo.findAllByQuizIdIgnoreCase(quizId)
        Map<Integer, List<QuizAnswerDef>> answerDefByQuestionId = dbAnswersDefs.groupBy {it.questionRefId }

        Set<Integer> selectedAnswerIds = quizAttemptAnswerRepo.getSelectedAnswerIds(quizAttemptId).toSet()

        List<QuizQuestionGradedResult> gradedQuestions = dbQuestionDefs.collect { QuizQuestionDef quizQuestionDef ->
            List<QuizAnswerDef> quizAnswerDefs = answerDefByQuestionId[quizQuestionDef.id]

            List<Integer> correctIds = quizAnswerDefs.findAll({ Boolean.valueOf(it.isCorrectAnswer) }).collect { it.id }.sort()
            List<Integer> selectedIds = quizAnswerDefs.findAll { selectedAnswerIds.contains(it.id) }?.collect { it.id }
            if (!selectedIds) {
                throw new SkillQuizException("There is no answer provided for question with [${quizQuestionDef.id}] id", quizId, ErrorCode.BadParam)
            }

            boolean isCorrect = selectedIds.containsAll(correctIds)

            UserQuizQuestionAttempt userQuizQuestionAttempt = new UserQuizQuestionAttempt(
                    userQuizAttemptRefId: quizAttemptId,
                    quizQuestionDefinitionRefId: quizQuestionDef.id,
                    userId: userId,
                    status: isCorrect ? UserQuizQuestionAttempt.QuizQuestionStatus.CORRECT : UserQuizQuestionAttempt.QuizQuestionStatus.WRONG,
            )
            quizQuestionAttemptRepo.save(userQuizQuestionAttempt)

            return new QuizQuestionGradedResult(questionId: quizQuestionDef.id, isCorrect: isCorrect, selectedAnswerIds: selectedIds, correctAnswerIds: correctIds)
        }
        boolean quizPassed = !gradedQuestions.find { !it.isCorrect }
        QuizGradedResult gradedResult = new QuizGradedResult(passed: quizPassed, gradedQuestions: gradedQuestions)

        userQuizAttempt.status = quizPassed ? UserQuizAttempt.QuizAttemptStatus.PASSED : UserQuizAttempt.QuizAttemptStatus.FAILED
        userQuizAttempt.completed = new Date()
        quizAttemptRepo.save(userQuizAttempt)

        gradedResult.associatedSkillResults = reportAnyAssociatedSkills(userQuizAttempt, quizDef)
        return gradedResult
    }

    @Profile
    List<SkillEventResult> reportAnyAssociatedSkills(UserQuizAttempt userQuizAttempt, QuizDef quizDef) {
        List<SkillEventResult> res = []
        if (userQuizAttempt.status == UserQuizAttempt.QuizAttemptStatus.PASSED) {
            List<QuizToSkillDefRepo.ProjectIdAndSkillId> skills = quizToSkillDefRepo.getSkillsForQuiz(quizDef.id)
            if (skills){
                skills.each {
                    SkillEventResult skillEventResult = skillEventsService.reportSkill(it.projectId, it.skillId, userQuizAttempt.userId, false, userQuizAttempt.completed,
                            new SkillEventsService.SkillApprovalParams(disableChecks: true))
                    res.add(skillEventResult)
                }
            }
        }
        return res
    }

    private QuizDef getQuizDef(String quizId) {
        QuizDef quizDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!quizDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return quizDef
    }
}
