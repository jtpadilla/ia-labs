package com.speedycontrol.labs.example.genai.study.batchmanagement;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.BatchJob;
import com.google.genai.types.BatchJobDestination;
import com.google.genai.types.BatchJobSource;
import com.google.genai.types.CreateBatchJobConfig;
import io.github.jtpadilla.gcloud.genai.IGenAIService;
import com.speedycontrol.labs.example.genai.common.GenAIServiceSelector;

public class BatchManagement {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Crea el batchJob
            final BatchJob created = create(genAIService.getLlmModel(), client);
            final BatchJobName batchJobName = BatchJobName.from(created);
            System.out.println("Se ha creado el batch job: " + created);

            // Get the batch job by name.
            final BatchJob getted = get(client, batchJobName);
            System.out.println("Get batch job: " + getted);

            // Cancel the batch job.
            cancel(client, batchJobName);
            System.out.println("Cancelled batch job: " + batchJobName.name());

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        } catch (BatchJobException e) {
            throw new RuntimeException(e);
        }

    }

    static private BatchJob create(String model, Client client) {

        final BatchJobSource batchJobSource =
                BatchJobSource.builder()
                        .gcsUri("gs://unified-genai-tests/batches/input/generate_content_requests.jsonl")
                        .format("jsonl")
                        .build();

        final BatchJobDestination batchJobDestination = BatchJobDestination.builder()
                .gcsUri("gs://unified-genai-tests/batches/output")
                .format("jsonl")
                .build();

        final CreateBatchJobConfig config =
                CreateBatchJobConfig.builder()
                        .displayName("summarize the pdf")
                        .dest(batchJobDestination)
                        .build();

        return client.batches.create(model, batchJobSource, config);

    }

    static private BatchJob get(Client client, BatchJobName batchJobName) throws BatchJobException {
        return client.batches.get(batchJobName.name(), null);
    }

    static private void cancel(Client client, BatchJobName batchJobName) throws BatchJobException {
        client.batches.cancel(batchJobName.name(), null);
    }

}
