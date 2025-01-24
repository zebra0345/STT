import React, { useState, useRef } from "react";

const Recorder = () => {
  const [transcript, setTranscript] = useState(""); // 최종 출력 텍스트
  const [isRecording, setIsRecording] = useState(false); // 녹음 상태
  const [eventSource, setEventSource] = useState(null); // SSE 연결 객체
  const lastFinalText = useRef(""); // 마지막 최종 처리된 텍스트 (중복 제거용)
  const buffer = useRef(""); // API에서 수신한 임시 텍스트 버퍼

  // 녹음 시작
  const startRecording = () => {
    if (!isRecording) {
      const source = new EventSource("http://localhost:8080/api/speech/stream");
      setEventSource(source);
      setIsRecording(true);

      // SSE 메시지 처리
      source.onmessage = (event) => {
        const newText = event.data.trim(); // 새로 수신된 텍스트
        const lastText = lastFinalText.current; // 마지막으로 처리된 텍스트

        // 중복 제거: 버퍼에 누적된 텍스트에서 중복된 부분 제거
        if (newText.startsWith(lastText)) {
          const uniquePart = newText.slice(lastText.length); // 중복된 부분 이후의 텍스트
          buffer.current += uniquePart;
        } else {
          // 새 텍스트가 중복되지 않은 경우, 버퍼에 추가
          buffer.current += newText;
        }

        // 최종 출력 업데이트
        setTranscript(buffer.current);

        // 최종 텍스트 업데이트
        lastFinalText.current = newText;
      };

      // SSE 에러 처리
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
      buffer.current = ""; // 버퍼 초기화
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
        rows="10"
        cols="50"
        value={transcript}
        readOnly
        style={{
          marginTop: "20px",
          width: "80%",
          height: "300px",
          fontSize: "16px",
          padding: "10px",
        }}
      />
    </div>
  );
};

export default Recorder;
