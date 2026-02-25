package io.github.jtpadilla.example.genai.study.batchmanagement;

import com.google.genai.types.BatchJob;

public record BatchJobName(String name) {

    static public BatchJobName from(BatchJob batchJob) throws BatchJobException {
        return new BatchJobName(
                batchJob.name().orElseThrow(()->new BatchJobException("No esta disponible en nombrte del Batch Job"))
        );
    }

}
