# Jarvis Bridge 1.2.5 — WAV xylophone originaux

La lecture SoundPool utilise exactement les quatre notes WAV approuvées
par l'utilisateur, et non une nouvelle synthèse au moment du statut.

Fichiers requis (PCM non compressé, mono, 16 bits, 44 100 Hz, ~0,54 s) :
- `Jarvis_Xylophone_Do5.wav`
- `Jarvis_Xylophone_Re5.wav`
- `Jarvis_Xylophone_Mi5.wav`
- `Jarvis_Xylophone_Sol5.wav`

Les WAV doivent être placés ici **avant de compiler**. Le script
`python3 tools/prepare_jarvis_wav.py` les valide et les copie vers les
ressources Android `res/raw/jarvis_{do5,re5,mi5,sol5}.wav`.

Le workflow GitHub est strictement manuel (`workflow_dispatch`) et
exécute cette validation avant de signer l'APK : une compilation sans
les quatre échantillons originaux est refusée plutôt que de produire
un APK silencieux ou de leur substituer d'autres notes.

La nouvelle interface contient le bouton « Tester les 4 notes » dans le
menu de la bulle (pression longue). Les statuts existants conservent
Do5 → Ré5 → Mi5 → Sol5.
