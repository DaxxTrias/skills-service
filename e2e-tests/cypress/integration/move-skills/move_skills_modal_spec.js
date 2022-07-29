/*
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
describe('Move Skills Modal Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
    });

    it('no destination', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillMoveBtn"]')
            .click();

        cy.contains('There are no Subjects or Groups that this skill can be moved to.');

        cy.get('[data-cy="destinationList"]')
            .should('not.exist');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="destListPagingControl"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .contains('No Destinations');
    });

    it('multiple skills - some are already reused', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.reuseSkillIntoAnotherSubject(1, 5, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillMoveBtn"]')
            .click();
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be moved to the [Subject 2] subject.');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have already been reused');

        cy.get('[data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 2 skills.');
        cy.get('[data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('multiple skills - first selected skill already moved', () => {
        // please note that this can only happen if user has multiple tabs open or another user moved the skills
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="selectAllSkillsBtn"]');

        cy.moveSkillIntoAnotherSubject(1, 1, 2);
        cy.moveSkillIntoAnotherSubject(1, 5, 2);

        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillMoveBtn"]')
            .click();
        cy.contains('Skills were moved or reused in another browser tab');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
    });

    it('multiple skills - some are already moved', () => {
        // please note that this can only happen if user has multiple tabs open or another user moved the skills
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="selectAllSkillsBtn"]');

        cy.moveSkillIntoAnotherSubject(1, 1, 2);
        cy.moveSkillIntoAnotherSubject(1, 5, 2);

        cy.get('[data-cy="skillSelect-skill3"]')
            .click({ force: true });
        cy.get('[data-cy="skillSelect-skill5"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillMoveBtn"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]');
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.exist');

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be moved to the [Subject 2] subject.');
        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.contains('Skills were moved or reused in another browser tab');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
    });

    it('cancel modal will focus on the Clear button', () => {
        cy.createSubject(1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillMoveBtn"]')
            .click();

        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="clearSelectedSkillsBtn"]')
            .should('have.focus');

        // close with X on top right
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillMoveBtn"]')
            .click();

        cy.get('[aria-label="Close"]')
            .click();
        cy.get('[data-cy="clearSelectedSkillsBtn"]')
            .should('have.focus');
    });

    it('if no skills were left after the move then New Skill button should get the focus', () => {
        cy.createSubject(1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillMoveBtn"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="newSkillButton"]')
            .should('have.focus');
    });

    it('successful reuse from a group should focus on the Select All button of its parent table', () => {
        cy.createSubject(1, 2);
        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="expandDetailsBtn_group11"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillSelect-skill6"]')
            .click({ force: true });
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillMoveBtn"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .should('have.focus');
    });
});
