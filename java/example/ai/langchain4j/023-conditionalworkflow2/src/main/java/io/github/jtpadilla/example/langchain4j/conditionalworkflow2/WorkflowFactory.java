package io.github.jtpadilla.example.langchain4j.conditionalworkflow2;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level1.categoryrouter.CategoryRouter;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level1.categoryrouter.CategoryRouterImpl;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level1.categoryrouter.RequestCategory;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level1.legalexpert.LegalExpert;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level1.legalexpert.LegalExpertImpl;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level1.medicalexpert.MedicalExpert;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level1.medicalexpert.MedicalExpertImpl;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.level2.TechnicalWorkflowImpl;

/**
 * Construye el pipeline de agentes con dos niveles de decisión:
 *
 * <pre>
 * ExpertRouterAgent (secuencia)
 *   ├─ level1.categoryrouter.CategoryRouter  → escribe "category" (LEGAL | MEDICAL | TECHNICAL | UNKNOWN)
 *   └─ Nivel 1 (condicional)
 *         ├─ MEDICAL   → level1.medicalexpert.MedicalExpert   (vía MedicalExpertImpl)
 *         ├─ LEGAL     → level1.legalexpert.LegalExpert       (vía LegalExpertImpl)
 *         └─ TECHNICAL → sub-flujo nivel 2                    (vía TechnicalWorkflowImpl)
 *               ├─ level2.engineeringrouter.EngineeringRouter → escribe "engineering_category"
 *               └─ Nivel 2 (condicional)
 *                     ├─ SOFTWARE   → level2.softwareengineer.SoftwareEngineer
 *                     ├─ HARDWARE   → level2.hardwareengineer.HardwareEngineer
 *                     ├─ CIVIL      → level2.civilengineer.CivilEngineer
 *                     └─ MECHANICAL → level2.mechanicalengineer.MechanicalEngineer
 * </pre>
 */
public class WorkflowFactory {

    private WorkflowFactory() {}

    public static ExpertRouterAgent build(ChatModel chatModel) {

        // ── Nivel 1: router de categoría principal ──────────────────────────
        CategoryRouter categoryRouter = CategoryRouterImpl.build(chatModel);

        // ── Expertos de nivel 1 ─────────────────────────────────────────────
        MedicalExpert medicalExpert = MedicalExpertImpl.build(chatModel);
        LegalExpert legalExpert = LegalExpertImpl.build(chatModel);

        // ── Sub-flujo técnico (nivel 2) ─────────────────────────────────────
        UntypedAgent technicalSubWorkflow = TechnicalWorkflowImpl.build(chatModel);

        // ── Condicional de nivel 1: dispatch por categoría principal ─────────
        UntypedAgent mainDispatcher = AgenticServices.conditionalBuilder()
                .subAgents(
                    scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL,
                    medicalExpert)
                .subAgents(
                    scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL,
                    legalExpert)
                .subAgents(
                    scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL,
                    technicalSubWorkflow)
                .build();

        // ── Secuencia principal: router de categoría → dispatch ──────────────
        return AgenticServices
                .sequenceBuilder(ExpertRouterAgent.class)
                .subAgents(categoryRouter, mainDispatcher)
                .outputKey("response")
                .build();
    }

}
