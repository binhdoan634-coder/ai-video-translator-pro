package com.avtpro.translator.data.api

import com.avtpro.translator.data.model.DubResponse
import com.avtpro.translator.data.model.JobStatusResponse
import com.avtpro.translator.data.model.ResultResponse
import com.avtpro.translator.data.model.SubtitleGenerateRequest
import com.avtpro.translator.data.model.TranscribeResponse
import com.avtpro.translator.data.model.TranslateRequest
import com.avtpro.translator.data.model.TranslateResponse
import com.avtpro.translator.data.model.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {

    @Multipart
    @POST("/api/video/upload")
    suspend fun uploadVideo(
        @Part file: MultipartBody.Part
    ): UploadResponse

    @POST("/api/video/translate")
    suspend fun translateVideo(
        @Body request: TranslateRequest
    ): TranslateResponse

    @GET("/api/jobs/{id}")
    suspend fun getJob(
        @Path("id") jobId: String
    ): JobStatusResponse

    @Multipart
    @POST("/api/audio/transcribe")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part
    ): TranscribeResponse

    @Multipart
    @POST("/api/audio/dub")
    suspend fun dubAudio(
        @Part file: MultipartBody.Part,
        @Part("target_language") targetLanguage: RequestBody,
        @Part("voice_mode") voiceMode: RequestBody,
        @Part("voice_similarity_consent") voiceSimilarity: RequestBody,
        @Part("voice_gender") voiceGender: RequestBody,
        @Part("output_format") format: RequestBody
    ): DubResponse

    @POST("/api/subtitles/generate")
    suspend fun generateSubtitles(
        @Body request: SubtitleGenerateRequest
    ): ResponseBody

    @GET("/api/video/result/{id}")
    suspend fun getResult(
        @Path("id") id: String
    ): ResultResponse

    @DELETE("/api/projects/{id}")
    suspend fun deleteProject(
        @Path("id") id: String
    ): ResponseBody

    /** Downloads any file URL returned by the backend (video/audio/subtitle). */
    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): ResponseBody

    /** Backend file download: /api/files/{id}/video|srt|vtt|ass|audio. */
    @Streaming
    @GET("/api/files/{id}/{kind}")
    suspend fun downloadResultFile(
        @Path("id") id: String,
        @Path("kind") kind: String
    ): ResponseBody
}
