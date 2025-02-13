import React, { useState, useRef } from "react";

const Recorder = () => {
  const [transcript, setTranscript] = useState(""); // 최종 출력 (10글자 유지)
  const [isRecording, setIsRecording] = useState(false); // 녹음 상태
  const [eventSource, setEventSource] = useState(null); // SSE 연결 객체
  const lastFinalText = useRef(""); // 마지막 확정된 텍스트 (중복 제거용)

  // 🔹 중복 제거 및 최신 10글자 유지 함수
  const extractNewText = (oldText, newText) => {
    if (newText.startsWith(oldText)) {
      return newText.slice(oldText.length); // 중복된 부분 제거
    }
    return newText; // 완전히 새로운 텍스트
  };

  // 녹음 시작
  const startRecording = () => {
    if (!isRecording) {
      const source = new EventSource("http://localhost:8080/api/speech/start");
      setEventSource(source);
      setIsRecording(true);

      source.onmessage = (event) => {
        const newText = event.data.trim(); // STT 결과
        const uniquePart = extractNewText(lastFinalText.current, newText); // 중복 제거
        lastFinalText.current = newText; // 최신 확정 문장 업데이트

        // 🔹 새로운 텍스트를 추가하고, 10글자만 유지
        setTranscript((prev) => {
          const updatedText = prev + uniquePart; // 누적
          return updatedText.slice(-10); // 최신 10글자 유지
        });
      };

      source.onerror = () => {
        console.error("SSE connection error");
        stopRecording();
      };
    }
  };

  // 녹음 종료
  const stopRecording = () => {
    if (isRecording && eventSource) {
      eventSource.close();
      setEventSource(null);
      setIsRecording(false);
      lastFinalText.current = ""; // 상태 초기화
      setTranscript(""); // 화면 표시 초기화
    }
  };

  return (
    <div style={{ textAlign: "center", marginTop: "50px" }}>
      <h1>Real-Time Speech-to-Text</h1>
      <div style={{ marginBottom: "20px" }}>
        <button
          onClick={startRecording}
          disabled={isRecording}
          style={{
            padding: "10px 20px",
            marginRight: "10px",
            backgroundColor: isRecording ? "gray" : "green",
            color: "white",
            border: "none",
            borderRadius: "5px",
            cursor: isRecording ? "not-allowed" : "pointer",
          }}
        >
          Start
        </button>
        <button
          onClick={stopRecording}
          disabled={!isRecording}
          style={{
            padding: "10px 20px",
            backgroundColor: !isRecording ? "gray" : "red",
            color: "white",
            border: "none",
            borderRadius: "5px",
            cursor: !isRecording ? "not-allowed" : "pointer",
          }}
        >
          Stop
        </button>
      </div>
      <textarea
        rows="2"
        cols="50"
        value={transcript}
        readOnly
        style={{
          marginTop: "20px",
          width: "80%",
          height: "50px",
          fontSize: "20px",
          padding: "10px",
          textAlign: "center",
        }}
      />
    </div>
  );
};

export default Recorder;
