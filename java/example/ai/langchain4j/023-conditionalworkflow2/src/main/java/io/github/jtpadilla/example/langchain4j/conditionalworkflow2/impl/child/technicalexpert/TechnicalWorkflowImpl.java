package io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.civilengineer.CivilEngineer;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.civilengineer.CivilEngineerImpl;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.hardwareengineer.HardwareEngineer;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.hardwareengineer.HardwareEngineerImpl;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.mechanicalengineer.MechanicalEngineer;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.mechanicalengineer.MechanicalEngineerImpl;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.softwareengineer.SoftwareEngineer;
import io.github.jtpadilla.example.langchain4j.conditionalworkflow2.impl.child.technicalexpert.child.softwareengineer.SoftwareEngineerImpl;

public class TechnicalWorkflowImpl {

    static public UntypedAgent build(ChatModel chatModel) {

        // ── Router de disciplina de ingeniería ──────────────────────────────
        EngineeringRouter engineeringRouter = EngineeringRouterImpl.build(chatModel);

        // ── Expertos de nivel 2 ─────────────────────────────────────────────
        SoftwareEngineer softwareEngineer = SoftwareEngineerImpl.build(chatModel);
        HardwareEngineer hardwareEngineer = HardwareEngineerImpl.build(chatModel);
        CivilEngineer civilEngineer = CivilEngineerImpl.build(chatModel);
        MechanicalEngineer mechanicalEngineer = MechanicalEngineerImpl.build(chatModel);

        // ── Condicional de nivel 2: dispatch por disciplina ─────────────────
        UntypedAgent engineeringDispatcher = AgenticServices.conditionalBuilder()
                .subAgents(
                    scope -> scope.readState("engineering_category", EngineeringCategory.UNKNOWN) == EngineeringCategory.SOFTWARE,
                    softwareEngineer)
                .subAgents(
                    scope -> scope.readState("engineering_category", EngineeringCategory.UNKNOWN) == EngineeringCategory.HARDWARE,
                    hardwareEngineer)
                .subAgents(
                    scope -> scope.readState("engineering_category", EngineeringCategory.UNKNOWN) == EngineeringCategory.CIVIL,
                    civilEngineer)
                .subAgents(
                    scope -> scope.readState("engineering_category", EngineeringCategory.UNKNOWN) == EngineeringCategory.MECHANICAL,
                    mechanicalEngineer)
                .build();

        // ── Secuencia: router de ingeniería → dispatch ──────────────────────
        return AgenticServices
                .sequenceBuilder(UntypedAgent.class)
                .subAgents(engineeringRouter, engineeringDispatcher)
                .outputKey("response")
                .build();
    }

}
