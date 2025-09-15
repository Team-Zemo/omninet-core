from flask import Flask, request, send_file, abort, jsonify
from TTS.api import TTS
import whisper
import soundfile as sf
import io

app = Flask(__name__)
tts = TTS(model_name="tts_models/en/vctk/vits", progress_bar=False, gpu=False)
whisper_model = whisper.load_model("small") # better accuracy, but can be changed to "tiny", "base", "small", "medium", "large"
@app.route('/tts', methods=['POST'])
def tts_api():
    if request.remote_addr not in ['127.0.0.1', '::1']:
        abort(403)
    text = request.json.get('text', '')
    if not text:
        abort(400, 'No text provided')
    speaker = tts.speakers[4]  # Use the first available speaker
    wav = tts.tts(text=text, speaker=speaker)
    buf = io.BytesIO()
    sf.write(buf, wav, tts.synthesizer.output_sample_rate, format='WAV')
    buf.seek(0)
    return send_file(buf, mimetype='audio/wav')

@app.route('/stt', methods=['POST'])
def stt_api():
    if request.remote_addr not in ['127.0.0.1', '::1']:
        abort(403)
    if 'audio' not in request.files:
        abort(400, 'No audio file provided')
    audio_file = request.files['audio']
    audio_bytes = audio_file.read()
    import tempfile
    with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
        tmp.write(audio_bytes)
        tmp.flush()
        result = whisper_model.transcribe(tmp.name)
    return jsonify({'text': result['text']})

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000)