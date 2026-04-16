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

/**
 * Sub-flujo técnico de nivel 2.
 *
 * <p>Se activa cuando el dispatcher de nivel 1 detecta {@code category == TECHNICAL}.
 * Internamente ejecuta una segunda secuencia:
 * {@link EngineeringSelector} clasifica la disciplina → dispatcher condicional de nivel 2
 * delega en el ingeniero especializado ({@link SoftwareEngineer}, {@link HardwareEngineer},
 * {@link CivilEngineer} o {@link MechanicalEngineer}).
 * El resultado final se escribe bajo la clave {@code "response"}.
 */
public class EngineeringRouterImpl {

    static public UntypedAgent build(ChatModel chatModel) {

        // ── Router de disciplina de ingeniería ──────────────────────────────
        EngineeringSelector engineeringSelector = EngineeringSelectorImpl.build(chatModel);

        // ── Expertos de nivel 2 ─────────────────────────────────────────────
        SoftwareEngineer softwareEngineer = SoftwareEngineerImpl.build(chatModel);
        HardwareEngineer hardwareEngineer = HardwareEngineerImpl.build(chatModel);
        CivilEngineer civilEngineer = CivilEngineerImpl.build(chatModel);
        MechanicalEngineer mechanicalEngineer = MechanicalEngineerImpl.build(chatModel);

        // ── Condicional de nivel 2: dispatch por disciplina ─────────────────
        UntypedAgent engineeringDispatcher = AgenticServices.conditionalBuilder()
                .subAgents(
                    scope -> scope.readState("engineering_category", EngineeringSelectorResult.UNKNOWN) == EngineeringSelectorResult.SOFTWARE,
                    softwareEngineer)
                .subAgents(
                    scope -> scope.readState("engineering_category", EngineeringSelectorResult.UNKNOWN) == EngineeringSelectorResult.HARDWARE,
                    hardwareEngineer)
                .subAgents(
                    scope -> scope.readState("engineering_category", EngineeringSelectorResult.UNKNOWN) == EngineeringSelectorResult.CIVIL,
                    civilEngineer)
                .subAgents(
                    scope -> scope.readState("engineering_category", EngineeringSelectorResult.UNKNOWN) == EngineeringSelectorResult.MECHANICAL,
                    mechanicalEngineer)
                .build();

        // ── Secuencia: router de ingeniería → dispatch ──────────────────────
        return AgenticServices
                .sequenceBuilder(UntypedAgent.class)
                .subAgents(engineeringSelector, engineeringDispatcher)
                .outputKey("response")
                .build();
    }

}
