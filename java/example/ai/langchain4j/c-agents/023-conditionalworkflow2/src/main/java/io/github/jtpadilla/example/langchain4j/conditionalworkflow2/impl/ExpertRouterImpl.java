package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.AgentMonitor;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.legalexpert.LegalExpert;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.legalexpert.LegalExpertImpl;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.medicalexpert.MedicalExpert;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.medicalexpert.MedicalExpertImpl;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.EngineeringRouterImpl;

/**
 * Construye el pipeline de agentes con dos niveles de decisión:
 *
 * <pre>
 * ExpertRouterAgent (secuencia)
 *   ├─ level0.categoryrouter.CategoryRouter  → escribe "category" (LEGAL | MEDICAL | TECHNICAL | UNKNOWN)
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
public class ExpertRouterImpl {

    private ExpertRouterImpl() {}

    public static ExpertRouter build(ChatModel chatModel, AgentMonitor agentMonitor) {

        // Tres crean los agentes expertos en las materias soportadas
        MedicalExpert medicalExpert = MedicalExpertImpl.build(chatModel);
        LegalExpert legalExpert = LegalExpertImpl.build(chatModel);
        UntypedAgent technicalSubWorkflow = EngineeringRouterImpl.build(chatModel);

        // Se crea el agente que determina la materia de la solicitud
        ExpertSelector expertSelector = ExpertSelectorImpl.build(chatModel);

        // Se crea el agente dispatcher condicional que en funcion de lo que este informado en el AgenticScope redirige la peticion
        UntypedAgent mainDispatcher = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState("category", ExpertSelectorResult.UNKNOWN) == ExpertSelectorResult.MEDICAL, medicalExpert)
                .subAgents(scope -> scope.readState("category", ExpertSelectorResult.UNKNOWN) == ExpertSelectorResult.LEGAL, legalExpert)
                .subAgents(scope -> scope.readState("category", ExpertSelectorResult.UNKNOWN) == ExpertSelectorResult.TECHNICAL, technicalSubWorkflow)
                .build();

        // Se secuencian los dis agentes:
        //   - El primero deposita en la variable "category" sus conclusiones.
        //   - El segundo en funcion de esta variable redirige al agente corespondiente.
        return AgenticServices
                .sequenceBuilder(ExpertRouter.class)
                .listener(agentMonitor)
                .subAgents(expertSelector, mainDispatcher)
                .outputKey("response")
                .build();

    }

}
