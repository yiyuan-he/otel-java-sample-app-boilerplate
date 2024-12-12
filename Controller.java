package otel;

import java.util.Optional;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.GetEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.DescribeActivityRequest;
import software.amazon.awssdk.services.sfn.model.DescribeStateMachineRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.GetGuardrailRequest;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.model.GetKnowledgeBaseRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetKnowledgeBaseResponse;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@RestController
public class Controller {
  private static final Logger logger = LoggerFactory.getLogger(Controller.class);

  public Controller() {
    System.out.println("Starting app");
  }

  @GetMapping("/rolldice")
  public String index(@RequestParam("player") Optional<String> player) {
    int result = this.getRandomNumber(1, 6);
    if (player.isPresent()) {
      logger.info("{} is rolling the dice: {}", player.get(), result);
    } else {
      logger.info("Anonymous player is rolling the dice: {}", result);
    }
    return Integer.toString(result);
  }

  public int getRandomNumber(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  @GetMapping("/createBucket")
  @ResponseBody
  public void createBucket() {
    String bucketName = "yiyuanh-test-create-bucket-us-east-1";

    try {
      // Create the S3 Client
      S3Client s3Client = S3Client.builder()
          .region(Region.US_EAST_1).build();

      // Create CreateBucketRequest
      CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
          .bucket(bucketName)
          .build();

      // Create the S3 bucket
      CreateBucketResponse createBucketResponse = s3Client.createBucket(createBucketRequest);

      System.out.println("Bucket created successfully");
      System.out.println("Bucket location: " + createBucketResponse.location());
    } catch (S3Exception e) {
      System.err.println("Error creating bucket: " + e.getMessage());
      System.exit(1);
    }
  }

  @GetMapping("/getBucketLocation")
  @ResponseBody
  public String s3GetBucketLocation() {
    String bucketName = "yiyuanh-otel-test-bucket";
    S3Client s3Client = S3Client.builder().region(Region.US_WEST_2).build();
    GetBucketLocationRequest locationRequest = GetBucketLocationRequest.builder().bucket(bucketName).build();
    s3Client.getBucketLocation(locationRequest);
    return "served";
  }

  @GetMapping("/describeStateMachine")
  @ResponseBody
  public String sfnDescribeStateMachine() {
    String stateMachineArn = "arn:aws:states:us-west-2:445567081046:stateMachine:MyStateMachine-s43028xq4";
    SfnClient sfnClient = SfnClient.builder().region(Region.US_WEST_2).build();
    DescribeStateMachineRequest request = DescribeStateMachineRequest.builder().stateMachineArn(stateMachineArn).build();
    sfnClient.describeStateMachine(request);
    return "served";
  }

  @GetMapping("/describeActivity")
  @ResponseBody
  public String sfnDescribeActivity() {
    String activityArn = "arn:aws:states:us-west-2:445567081046:activity:testActivity";
    SfnClient sfnClient = SfnClient.builder().region(Region.US_WEST_2).build();
    DescribeActivityRequest request = DescribeActivityRequest.builder().activityArn(activityArn).build();
    sfnClient.describeActivity(request);
    return "served";
  }

  @GetMapping("/receiveMessage")
  @ResponseBody
  public String sqsReceive() {
    SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1).build();
    String queueUrl = "https://sqs.us-east-1.amazonaws.com/445567081046/yiyuanh-us-east-1-test-queue";
    System.out.println("queue_url: " + queueUrl);
    try {
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .build();
        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).messages();
        if (messages.size() > 0) {
            System.out.println("message= " + messages.get(0).body());
        }
    } catch (SqsException e) {
        System.out.println(e.awsErrorDetails().errorMessage());
        throw e;
    }
    return "served";
  }

  @GetMapping("/publish")
  @ResponseBody
  public String snsPublish() {
    String topicArn = "arn:aws:sns:us-west-2:445567081046:TestTopic";
    String message = "Hello from AWS SNS!";
    SnsClient snsClient = SnsClient.builder().region(Region.US_WEST_2).build();
    PublishRequest publishRequest = PublishRequest.builder().topicArn(topicArn).message(message).build();
    snsClient.publish(publishRequest);
    return "served";
  }

  @GetMapping("/getSecretValue")
  @ResponseBody
  public String secretsManagerGetSecretValue() {
    String secretArn = "arn:aws:secretsmanager:us-west-2:445567081046:secret:rds!cluster-9a14ac22-6137-43c3-b131-7c6513cc668b-OWo6Gd";
    String secretArn2 = "arn:aws:secretsmanager:us-west-2:445567081046:secret:test_plain_text_secret-rUOvnc";
    SecretsManagerClient secretsClient = SecretsManagerClient.builder().region(Region.US_WEST_2).build();
    GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretArn).build();
    secretsClient.getSecretValue(getSecretValueRequest);
    return "served";
  }

  @GetMapping("/getFunction")
  @ResponseBody
  public String lambdaGetFunction() {
    String functionName = "yiyuanh-test-otel-lambda";
    String functionArn = "arn:aws:lambda:us-west-2:445567081046:function:yiyuanh-test-otel-lambda";
    LambdaClient lambdaClient = LambdaClient.builder().region(Region.US_WEST_2).build();
    GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder().functionName(functionArn).build();
    lambdaClient.getFunction(getFunctionRequest);
    return "served";
  }

  @GetMapping("/getEventSourceMapping")
  @ResponseBody
  public String lambdaGetEventSourceMapping() {
    String uuid = "a7f25f34-ad44-480f-8385-4c0affee4d1b";
    LambdaClient lambdaClient = LambdaClient.builder().region(Region.US_WEST_2).build();
    GetEventSourceMappingRequest request = GetEventSourceMappingRequest.builder().uuid(uuid).build();
    lambdaClient.getEventSourceMapping(request);
    return "served";
  }

  @GetMapping("/getGuardrail")
  @ResponseBody
  public String bedrockGetGuardrail() {
    String guardrailId = "ml2x8svlammy";
    BedrockClient bedrockClient = BedrockClient.builder().region(Region.US_WEST_2).build();
    GetGuardrailRequest request = GetGuardrailRequest.builder().guardrailIdentifier(guardrailId).build();
    bedrockClient.getGuardrail(request);
    return "served";
  }

  @GetMapping("/getKnowledgeBase")
  @ResponseBody
  public String bedrockGetKnowledgeBase() {
    String knowledgeBaseId = "VVTY3IRMU6";
    BedrockAgentClient bedrockAgentClient = BedrockAgentClient.builder().region(Region.US_WEST_2).build();
    GetKnowledgeBaseRequest getKnowledgeBaseRequest = GetKnowledgeBaseRequest.builder().knowledgeBaseId(knowledgeBaseId).build();
    GetKnowledgeBaseResponse getKnowledgeBaseResponse = bedrockAgentClient.getKnowledgeBase(getKnowledgeBaseRequest);
    return "served";
  }

  @GetMapping("/invokeAi21JambaModel")
  @ResponseBody
  public String invokeAi21Model() {
    var client = BedrockRuntimeClient.builder()
      .region(Region.US_EAST_1)
      .build();
    var modelId = "ai21.jamba-1-5-mini-v1:0";
    JSONObject message = new JSONObject()
      .put("role", "user")
      .put("content", "Which LLM are you?");
    JSONArray messages = new JSONArray()
      .put(message);
    JSONObject requestBody = new JSONObject()
      .put("messages", messages)
      .put("max_tokens", 1000)
      .put("top_p", 0.8)
      .put("temperature", 0.7);

    try {
      var response = client.invokeModel(request -> request
        .body(SdkBytes.fromUtf8String(requestBody.toString()))
        .modelId(modelId)
      );
      var responseBody = new JSONObject(response.body().asUtf8String());
      var text = new JSONPointer("/choices/0/message/content").queryFrom(responseBody).toString();
      System.out.println(text);
      return text;
    } catch (SdkClientException e) {
        System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", modelId, e.getMessage());
        throw new RuntimeException(e);
    }
  }

  @GetMapping("/invokeAmazonTitanModel")
  @ResponseBody
  public String invokeAmazonTitan() {
    var client = BedrockRuntimeClient.builder()
      .region(Region.US_EAST_1)
      .build();
    var modelId = "amazon.titan-text-premier-v1:0";
    var prompt = "Describe the purpose of a 'hello world' program in one line.";
    JSONObject textGenerationConfig = new JSONObject()
      .put("temperature", 0.75)
      .put("topP", 0.9)
      .put("maxTokenCount", 512);
    JSONObject requestBody = new JSONObject()
      .put("inputText", prompt)
      .put("textGenerationConfig", textGenerationConfig);
    try {
      var response = client.invokeModel(request -> request
        .body(SdkBytes.fromUtf8String(requestBody.toString()))
        .modelId(modelId)
      );
      var responseBody = new JSONObject(response.body().asUtf8String());
      var text = new JSONPointer("/results/0/outputText").queryFrom(responseBody).toString();
      System.out.println(text);
      return text;
    } catch (SdkClientException e) {
      System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", modelId, e.getMessage());
      throw new RuntimeException();
    }
  }

  @GetMapping("/invokeAnthropicClaudeModel")
  @ResponseBody
  public String invokeAnthropicClaude() {
    var client = BedrockRuntimeClient.builder()
      .region(Region.US_EAST_1)
      .build();
    var modelId = "anthropic.claude-3-haiku-20240307-v1:0";
    var prompt = "Describe the purpose of a compiler in one line.";
    JSONObject message = new JSONObject()
      .put("role", "user")
      .put("content", "Describe a cache in one line");
    JSONArray messages = new JSONArray().put(message);
    JSONObject requestBody = new JSONObject()
      .put("anthropic_version", "bedrock-2023-05-31")
      .put("max_tokens", 512)
      .put("top_p", 0.53)
      .put("temperature", 0.6)
      .put("messages", messages);

    try {
        // Encode and send the request to the Bedrock Runtime.
        var response = client.invokeModel(request -> request
                .body(SdkBytes.fromUtf8String(requestBody.toString()))
                .modelId(modelId)
        );

        // Decode the response body.
        var responseBody = new JSONObject(response.body().asUtf8String());

        // Retrieve the generated text from the model's response.
        var text = new JSONPointer("/content/0/text").queryFrom(responseBody).toString();
        System.out.println(text);

        return text;

    } catch (SdkClientException e) {
        System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", modelId, e.getMessage());
        throw new RuntimeException(e);
    }
  }

  @GetMapping("/invokeCohereCommandModel")
  @ResponseBody
  public String invokeCohereCommand() {
    var client = BedrockRuntimeClient.builder()
      .region(Region.US_EAST_1)
      .build();
    var modelId = "cohere.command-light-text-v14";
    var prompt = "Describe the purpose of an interpreter in one line.";
    var nativeRequestTemplate = """
        {
            "prompt": "{{prompt}}",
            "max_tokens": 4096,
            "temperature": 0.75,
            "p": 0.25
        }""";
    String nativeRequest = nativeRequestTemplate.replace("{{prompt}}", prompt);

    try {
      var response = client.invokeModel(request -> request
        .body(SdkBytes.fromUtf8String(nativeRequest))
        .modelId(modelId)
      );
      var responseBody = new JSONObject(response.body().asUtf8String());
      var text = new JSONPointer("/generations/0/text").queryFrom(responseBody).toString();
      System.out.println(text);
      return text;
    } catch (SdkClientException e) {
        System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", modelId, e.getMessage());
        throw new RuntimeException(e);
    }
  }

  @GetMapping("/invokeCohereCommandModelR")
  @ResponseBody
  public String invokeCohereCommandR() {
    var client = BedrockRuntimeClient.builder()
      .region(Region.US_EAST_1)
      .build();
    var modelId = "cohere.command-r-v1:0";
    var prompt = "Describe the purpose of an interpreter in one line.";
    var nativeRequestTemplate = """
        {
            "message": "{{prompt}}",
            "max_tokens": 4096,
            "temperature": 0.75,
            "p": 0.25
        }""";
    String nativeRequest = nativeRequestTemplate.replace("{{prompt}}", prompt);

    try {
      var response = client.invokeModel(request -> request
        .body(SdkBytes.fromUtf8String(nativeRequest))
        .modelId(modelId)
      );
      var responseBody = new JSONObject(response.body().asUtf8String());
      var text = new JSONPointer("/text").queryFrom(responseBody).toString();
      System.out.println(text);
      return text;
    } catch (SdkClientException e) {
        System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", modelId, e.getMessage());
        throw new RuntimeException(e);
    }
  }

  @GetMapping("/invokeMetaLlamaModel")
  @ResponseBody
  public String invokeMetaModel() {
    var client = BedrockRuntimeClient.builder()
      .region(Region.US_EAST_1)
      .build();
    var modelId = "meta.llama3-70b-instruct-v1:0";
    var prompt = "Describe the purpose of a 'hello world' program in one line.";
    var instruction = (
                "<|begin_of_text|><|start_header_id|>user<|end_header_id|>\\n" +
                "{{prompt}} <|eot_id|>\\n" +
                "<|start_header_id|>assistant<|end_header_id|>\\n"
    ).replace("{{prompt}}", prompt);
    JSONObject requestBody = new JSONObject()
      .put("prompt", instruction)
      .put("max_gen_len", 128)
      .put("temperature", 0.1)
      .put("top_p", 0.9);

    try {
        // Encode and send the request to the Bedrock Runtime.
        var response = client.invokeModel(request -> request
                .body(SdkBytes.fromUtf8String(requestBody.toString()))
                .modelId(modelId)
        );

        // Decode the response body.
        var responseBody = new JSONObject(response.body().asUtf8String());

        // Retrieve the generated text from the model's response.
        var text = new JSONPointer("/generation").queryFrom(responseBody).toString();
        System.out.println(text);

        return text;

    } catch (SdkClientException e) {
        System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", modelId, e.getMessage());
        throw new RuntimeException(e);
    }
  }

  @GetMapping("/invokeMistralAiModel")
  @ResponseBody
  public String invokeMistralModel() {
    var client = BedrockRuntimeClient.builder()
      .region(Region.US_EAST_1)
      .build();
    var modelId = "mistral.mistral-large-2402-v1:0";
    var prompt = "Describe the difference between a compiler and interpreter in one line.";
    var instruction = "<s>[INST] {{prompt}} [/INST]\\n".replace("{{prompt}}", prompt);
    var nativeRequestTemplate = """
        {
            "prompt": "{{instruction}}",
            "max_tokens": 4096,
            "temperature": 0.75,
            "top_p": 0.25
        }""";
    String nativeRequest = nativeRequestTemplate.replace("{{instruction}}", instruction);

    try {
      var response = client.invokeModel(request -> request
        .body(SdkBytes.fromUtf8String(nativeRequest))
        .modelId(modelId)
      );
      var responseBody = new JSONObject(response.body().asUtf8String());
      var text = new JSONPointer("/outputs/0/text").queryFrom(responseBody).toString();
      System.out.println(text);
      return text;
    } catch (SdkClientException e) {
        System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", modelId, e.getMessage());
        throw new RuntimeException(e);
    }
  }

  @GetMapping("/invokeAmazonNovaModel")
  @ResponseBody
  public String invokeAmazonNovaModel() {
    var client = BedrockRuntimeClient.builder()
      .region(Region.US_EAST_1)
      .build();
    var modelId = "us.amazon.nova-lite-v1:0";

    // Define the system prompt(s)
    JSONArray system = new JSONArray().put(
        new JSONObject().put("text", "Act as a creative writing assistant. When the user provides you with a topic, write a short story about that topic.")
    );

    // Define user input message(s)
    JSONArray messages = new JSONArray().put(
        new JSONObject()
            .put("role", "user")
            .put("content", new JSONArray().put(new JSONObject().put("text", "A camping trip")))
    );

    // Configure inference parameters
    JSONObject inferenceConfig = new JSONObject()
        .put("max_new_tokens", 500)
        .put("top_p", 0.9)
        .put("top_k", 20)
        .put("temperature", 0.7);

    // Create request body
    JSONObject requestBody = new JSONObject()
        .put("schemaVersion", "messages-v1")
        .put("messages", messages)
        .put("system", system)
        .put("inferenceConfig", inferenceConfig);

    try {
      // Send the request to Bedrock Runtime
      var response = client.invokeModel(request -> request
        .body(SdkBytes.fromUtf8String(requestBody.toString()))
        .modelId(modelId)
      );

      // Parse the response body
      var responseBody = new JSONObject(response.body().asUtf8String());
      System.out.println("Response Body: " + responseBody);
      return "";
    } catch (SdkClientException e){
      System.err.printf("ERROR: Can't invoke '%s'. Reason: %s%n", modelId, e.getMessage());
      throw new RuntimeException(e);
    }
  }
}


