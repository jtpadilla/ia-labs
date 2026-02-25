# Agent Instructions

This document provides instructions for agents working with this repository.

## Project Goal

The objective of this repository is to experiment with Google's Gemini Generative AI, exploring its practical uses by integrating it into other developments. This repository is experimental and may undergo drastic refactoring. The project relies heavily on Google Cloud products.

## Technologies

*   **Java**: The primary programming language.
*   **Bazel**: The build system used for compiling the project, managing dependencies, and ensuring reproducible builds.
*   **Protocol Buffers (Protobuf)**: Used for data structure definition and efficient serialization.

## Repository Structure

| Directory | Content |
|---|---|
| `/java` | All Java source code. |
| `/java/example` | Examples of how to use the various technologies and APIs. |
| `/java/platform` | Reusable components that can be wired with products through dependency injection. |
| `/java/product` | Generated products or applications. |
| `/proto` | Protobuf (`.proto`) file definitions. |

## Building the Project

To build the entire project, run the following command from the root of the repository:

```bash
bazel build //...
```

## Running Examples

The examples are designed to run on Google Cloud infrastructure and require access to a Google Cloud project.

### GCloud Configuration

1.  **Service Account**: You need a service account with a JSON key file.
2.  **Configuration File**: Create a file at `$HOME/.iatevale/config.properties`.
3.  **Credential File**: Place your JSON credential file in a secure location. It is recommended to place it in the `$HOME/.iatevale/` directory.

**Example `config.properties` file:**

```properties
project.id=your-gcloud-project-id
credentials=/home/{your-username}/.iatevale/your-credential-file.json
```

**Required IAM Roles**:

The service account must have the following roles assigned:

*   Storage Admin (`roles/storage.admin`)
*   Cloud Datastore User (`roles/datastore.user`)
*   Vertex AI User (`roles/aiplatform.user`)

### Running a specific example

Use `bazel run` to execute an example. For instance, to run a storage example, you would use a command similar to this:

```bash
# The exact target will depend on the example's BUILD.bazel file
bazel run //java/example/gcloud/storage:ExampleName
```

## Deploying an Agent to Google Cloud Run

This section outlines the process for deploying an agent as a job to Google Cloud Run.

1.  **Install and Initialize gcloud CLI**:
    *   [Install the gcloud CLI](https://cloud.google.com/sdk/docs/install-sdk).
    *   Initialize it by running `gcloud init` and following the prompts to connect to your project.

2.  **Configure Docker Authentication**:
    *   Configure Docker to authenticate with Artifact Registry. Replace `europe-west4` with the region of your repository if it's different.
    ```bash
    gcloud auth configure-docker europe-west4-docker.pkg.dev
    ```

3.  **Update the BUILD.bazel file**:
    *   Locate the `BUILD.bazel` file for the product you want to deploy (e.g., `java/product/iatevaleagent/BUILD.bazel`).
    *   Find the `oci_push` rule and update the `repository` attribute with your Google Cloud `project-id` and Artifact Registry `repo-name`.

    **Example `oci_push` rule in `java/product/iatevaleagent/BUILD.bazel`:**
    ```bazel
    oci_push(
        name = "iatevaleagent_gcloud_push",
        image = ":iatevaleagent_gcloud_image",
        # Replace project-id and repo-name with your values
        repository = "europe-west4-docker.pkg.dev/project-id/repo-name/iatevaleagent",
    )
    ```

4.  **Build and Push the OCI Image**:
    *   Run the `oci_push` rule using Bazel. This will build the image and push it to your Artifact Registry.
    ```bash
    bazel run //java/product/iatevaleagent:iatevaleagent_gcloud_push
    ```
    *   After the command succeeds, it will print the image digest (a `sha256:...` hash). Copy this digest for the next step.

5.  **Create and Execute the Cloud Run Job**:
    *   Use the `gcloud` CLI to create and execute a job with the image you just pushed.
    *   Replace `project-id`, `repo-name`, and the image digest in the command below.

    ```bash
    # Create the job
    gcloud run jobs create iatevaleagent-job \
        --image=europe-west4-docker.pkg.dev/project-id/repo-name/iatevaleagent@sha256:YOUR_IMAGE_DIGEST_HERE \
        --project=project-id

    # Execute the job
    gcloud run jobs execute iatevaleagent-job
    ```

## Useful Links

### Core Google Cloud & AI Documentation
*   [Google AI Platform](https://ai.google.dev)
*   [Google Cloud APIs](https://cloud.google.com/apis)
*   [Gemini API Documentation](https://ai.google.dev/gemini-api)
*   [Gemini Cookbook on GitHub](https://github.com/google-gemini/cookbook)
*   [Gemma Open Models](https://ai.google.dev/gemma)

### Java Libraries for Google Cloud
*   [Google Cloud Java Documentation](https://cloud.google.com/java/docs)
*   [Google Cloud Java Reference](https://cloud.google.com/java/docs/reference)
*   [Google Cloud Java SDK GitHub](https://github.com/googleapis/google-cloud-java)
*   [Vertex AI for Java on GitHub](https://github.com/googleapis/google-cloud-java/tree/main/java-vertexai)
*   [Vertex AI for Java Samples](https://github.com/googleapis/google-cloud-java/tree/main/java-aiplatform/samples/snippets/generated/com/google/cloud/aiplatform/v1)

### Vertex AI Search (Discovery Engine)
*   [Vertex AI Search Java Docs](https://cloud.google.com/java/docs/reference/google-cloud-discoveryengine/latest/overview)
*   [Vertex AI Agent Builder Console](https://console.cloud.google.com/gen-app-builder)
*   [Vertex AI Search GitHub](https://github.com/googleapis/google-cloud-java/tree/main/java-discoveryengine)
