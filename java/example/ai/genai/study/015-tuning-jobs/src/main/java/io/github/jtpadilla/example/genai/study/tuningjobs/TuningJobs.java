package io.github.jtpadilla.example.genai.study.tuningjobs;

import com.google.genai.Client;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.TuningDataset;
import com.google.genai.types.TuningJob;
import io.github.jtpadilla.example.genai.common.GenAIServiceSelector;
import io.github.jtpadilla.gcloud.genai.IGenAIService;

// La primera ejecucion ha dado como resultado: projects/176809830110/locations/europe-southwest1/models/2132920616490106880@1

public class TuningJobs {

    public static void main(String[] args) {

        final IGenAIService genAIService = GenAIServiceSelector.fromDefault();

        try (Client client = genAIService.createClient()) {

            // Create a tuning job.
            final TuningDataset tuningDataset =
                    TuningDataset.builder()
                            .gcsUri(
                                    "gs://cloud-samples-data/ai-platform/generative_ai/gemini-1_5/text/sft_train_data.jsonl")
                            .build();
            final TuningJob tuningJob1 = client.tunings.tune(genAIService.getLlmModel(), tuningDataset, null);
            System.out.println("Created tuning job: " + tuningJob1);

            // Get the tuning job by name.
            final TuningJob tuningJob2 = client.tunings.get(tuningJob1.name().get(), null);
            System.out.println("Get tuning job: " + tuningJob2);

            // Wait for the tuned model to be available.
            String tunedModel = "";
            while (tunedModel.isEmpty()) {
                System.out.println("Waiting for tuned model to be available");
                try {
                    Thread.sleep(10000); // Sleep for 10 seconds.
                } catch (InterruptedException e) {
                    System.out.println("Thread was interrupted while sleeping.");
                    Thread.currentThread().interrupt();
                }
                // Get the tuning job.
                TuningJob fetchedTuningJob = client.tunings.get(tuningJob1.name().get(), null);
                if (fetchedTuningJob.tunedModel().isPresent()
                        && fetchedTuningJob.tunedModel().get().model().isPresent()) {
                    tunedModel = fetchedTuningJob.tunedModel().get().model().get();
                }
            }
            System.out.println("Tuned model: " + tunedModel);

        } catch (GenAiIOException e) {
            System.out.println("An error occurred working with GenAI: " + e.getMessage());
        }

    }

}
