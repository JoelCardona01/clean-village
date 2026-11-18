# -*- coding: utf-8 -*-
"""All player-facing texts of Clean Village (en_us + es_es). Used by gen_assets.py."""


def es_dirty(base):
    first = base.split(" ")[0].lower()
    if first.endswith("as"):
        adj = "sucias"
    elif first.endswith("os") or first.endswith("es"):
        adj = "sucios"
    elif first.endswith("a"):
        adj = "sucia"
    else:
        adj = "sucio"
    words = base.split(" ")
    # put the adjective after the noun phrase: "Tablones de roble sucios"
    return base + " " + adj


STORIES_ES = [
    # 0 flood
    ["Hola, quien lea esto.\n\nMe llamo %1$s. Hace unas semanas una tormenta terrible reventó el tejado y el agua entró por todas partes.",
     "Cuando por fin dejó de llover, todo estaba lleno de barro y el moho empezó a salir por las esquinas.\n\nNo sabía ni por dónde empezar...",
     "Desde entonces me paso el día en la cama, sin ganas de nada.\n\nEn este cofre guardo mis cosas de limpieza. Si me ayudas, no lo olvidaré nunca.\n\n- %1$s"],
    # 1 long trip
    ["Hola, viajero.\n\nSoy %1$s. Me fui un año entero a visitar a mi primo del desierto. ¡Qué arena, qué sol!",
     "Al volver, mi casa ya no era mía: telarañas, polvo hasta en el techo y las ventanas tan sucias que ni entra la luz.\n\nLas arañas hasta tenían nombres.",
     "No me quedan fuerzas ni para empezar.\n\nTe dejo aquí mis esponjas. ¿Me echas una mano?\n\n- %1$s"],
    # 2 illness
    ["Querido desconocido:\n\nSoy %1$s. Pasé todo el invierno en la cama con fiebre y sopa de remolacha.",
     "Mientras tanto, la casa se fue ensuciando poco a poco. Primero el polvo, luego las manchas, luego... eso verde de las esquinas.",
     "Ya estoy mejor, pero al ver todo así se me encoge el corazón.\n\nSi te sobra un rato, mis cosas de limpieza están en este cofre.\n\n- %1$s"],
    # 3 chimney fire
    ["Hola.\n\nMe llamo %1$s. Una noche se me olvidó limpiar la chimenea y... ¡fuuum! Salió humo negro por toda la casa.",
     "No se quemó nada, menos mal, pero el hollín lo cubrió todo: paredes, suelo, ventanas. Hasta mi gato salió gris.",
     "Intenté limpiarlo por mi cuenta, pero solo conseguí extender la mancha.\n\nTe dejo el kit de limpieza aquí. Por favor, ayúdame.\n\n- %1$s"],
    # 4 spider invasion
    ["Hola, valiente.\n\nSoy %1$s. Una noche entró una araña. Luego dos. Luego... bueno, perdí la cuenta.",
     "Se fueron solas cuando llegó el frío, pero dejaron telarañas en cada rincón y todo lleno de polvo y manchas.",
     "Me da tanta pena ver mi casa así que ya ni salgo a comerciar.\n\nEn este cofre tienes todo lo necesario. ¡La esponja quita hasta las telarañas!\n\n- %1$s"],
    # 5 pillagers
    ["A quien encuentre esto:\n\nSoy %1$s. Unos saqueadores asaltaron la aldea y se metieron en mi casa con las botas llenas de barro.",
     "Rompieron los farolillos, lo pisotearon todo y dejaron las paredes hechas un desastre. Ahora está oscuro y huele a pantano.",
     "Ya se fueron, pero yo sigo aquí sin saber qué hacer.\n\nSi pudieras ayudarme a dejarla como antes... el kit está en el cofre.\n\n- %1$s"],
]

STORIES_EN = [
    ["Hello, whoever reads this.\n\nMy name is %1$s. A few weeks ago a terrible storm burst through the roof and water got in everywhere.",
     "When the rain finally stopped, everything was covered in mud and mold started creeping out of the corners.\n\nI didn't know where to start...",
     "Since then I just sit on my bed all day.\n\nMy cleaning things are in this chest. If you help me, I will never forget it.\n\n- %1$s"],
    ["Hello, traveler.\n\nI'm %1$s. I spent a whole year visiting my cousin in the desert. So much sand, so much sun!",
     "When I came back, my house wasn't mine anymore: cobwebs, dust up to the ceiling and windows so dirty no light gets in.\n\nThe spiders even had names.",
     "I'm so tired I can't even begin.\n\nI'm leaving my sponges here. Could you give me a hand?\n\n- %1$s"],
    ["Dear stranger,\n\nI'm %1$s. I was sick all winter long, stuck in bed with a fever and beetroot soup.",
     "Meanwhile the house got dirtier and dirtier. First dust, then stains, then... that green stuff in the corners.",
     "I feel better now, but seeing it like this breaks my heart.\n\nIf you have a moment, my cleaning things are in this chest.\n\n- %1$s"],
    ["Hi.\n\nMy name is %1$s. One night I forgot to sweep the chimney and... whoosh! Black smoke filled the whole house.",
     "Nothing burned, thankfully, but soot covered everything: walls, floor, windows. Even my cat came out grey.",
     "I tried to clean it myself, but I only spread the mess around.\n\nI'm leaving the cleaning kit here. Please, help me.\n\n- %1$s"],
    ["Hello, brave one.\n\nI'm %1$s. One night a spider came in. Then two. Then... well, I lost count.",
     "They left when the cold came, but they left cobwebs in every corner and everything full of dust and stains.",
     "It makes me so sad to see my house like this that I don't even go out to trade.\n\nEverything you need is in this chest. The sponge even wipes away cobwebs!\n\n- %1$s"],
    ["To whoever finds this:\n\nI'm %1$s. Pillagers raided the village and stomped into my house with muddy boots.",
     "They broke the lanterns, trampled everything and left the walls a disaster. Now it's dark and smells like a swamp.",
     "They're gone now, but I'm still sitting here not knowing what to do.\n\nIf you could help me make it like before... the kit is in the chest.\n\n- %1$s"],
]

LETTERS_ES = [
    ["Querido/a %2$s:\n\n¡Mi casa brilla! Ya no queda ni una gota de barro, y el moho se ha ido a vivir a otra parte (espero que lejos).",
     "Gracias de todo corazón. Te guardo un sitio en mi mesa para siempre.\n\nCon cariño, y oliendo a jabón,\n%1$s"],
    ["Querido/a %2$s:\n\n¡Mira qué ventanas! Ahora entra tanto sol que parece que he vuelto al desierto, pero sin arena.",
     "Las arañas me escribieron para quejarse. Les dije que lo sentía mucho (no lo siento).\n\nGracias, de verdad.\n%1$s"],
    ["Querido/a %2$s:\n\nNo sabes lo que significa para mí volver a una casa limpia después de un invierno tan largo.",
     "Vuelvo a tener energía. Cuando quieras, pásate a por una sopa de remolacha. Esta vez sin fiebre.\n\nCon cariño,\n%1$s"],
    ["Querido/a %2$s:\n\n¡Adiós, hollín! Las paredes vuelven a tener su color y mi gato vuelve a ser naranja.",
     "Prometo limpiar la chimenea cada semana. Bueno, cada mes. Bueno... lo intentaré.\n\nMil gracias,\n%1$s"],
    ["Querido/a %2$s:\n\n¡Ni una telaraña! He mirado debajo de la cama tres veces y está todo reluciente.",
     "Eres la persona más valiente de toda la aldea. Si vuelven las arañas, ya sé a quién llamar.\n\nUn abrazo,\n%1$s"],
    ["Querido/a %2$s:\n\nMi casa vuelve a ser mi casa. Han vuelto los farolillos, la luz y hasta las ganas de cantar.",
     "Que tiemblen los saqueadores: ahora tengo una amistad que limpia más rápido que ellos ensucian.\n\nGracias,\n%1$s"],
]

LETTERS_EN = [
    ["Dear %2$s,\n\nMy house is sparkling! Not a single drop of mud left, and the mold has moved somewhere else (far away, I hope).",
     "Thank you from the bottom of my heart. There will always be a seat for you at my table.\n\nWith love, and smelling of soap,\n%1$s"],
    ["Dear %2$s,\n\nLook at those windows! So much sunlight gets in now that it feels like the desert again, but without the sand.",
     "The spiders wrote to complain. I told them I was very sorry (I'm not).\n\nThank you, truly.\n%1$s"],
    ["Dear %2$s,\n\nYou can't imagine what it means to come back to a clean home after such a long winter.",
     "I feel brand new. Drop by for some beetroot soup whenever you like. No fever this time.\n\nYour friend,\n%1$s"],
    ["Dear %2$s,\n\nGoodbye, soot! The walls have their color back and my cat is orange again.",
     "I promise to sweep the chimney every week. Well, every month. Well... I'll try.\n\nA thousand thanks,\n%1$s"],
    ["Dear %2$s,\n\nNot a single cobweb! I checked under the bed three times and it's all sparkling.",
     "You are the bravest person in the whole village. If the spiders come back, I know who to call.\n\nHugs,\n%1$s"],
    ["Dear %2$s,\n\nMy house is my home again. The lanterns are back, the light is back, even my wish to sing is back.",
     "Let the pillagers tremble: now I have a friend who cleans faster than they can make a mess.\n\nThank you,\n%1$s"],
]

SAD_ES = ["Mira cómo está todo... no tengo fuerzas.", "Antes esta casa brillaba, ¿sabes?", "Hmm... hoy no tengo ganas de comerciar.",
          "Ni siquiera encuentro mis farolillos.", "Lo siento, no estoy de humor...", "*suspiro*",
          "Si al menos alguien me ayudara a limpiar...", "En el cofre de al lado de la cama está todo lo que necesito... pero no puedo.",
          "Las manchas no se van solas, ¿verdad?", "Hmm... hmm..."]
SAD_EN = ["Look at this place... I have no strength left.", "This house used to sparkle, you know?", "Hmm... I don't feel like trading today.",
          "I can't even find my lanterns.", "Sorry, I'm not in the mood...", "*sigh*",
          "If only someone would help me clean...", "Everything I need is in the chest by the bed... but I can't.",
          "Stains don't go away on their own, do they?", "Hmm... hmm..."]
WAIT_ES = ["¡Ánimo! ¡Se nota la diferencia!", "Espero aquí fuera, no quiero estorbar.", "¿Ya va quedando bien? ¡Qué nervios!",
           "¡Con la esponja sale todo, hasta las telarañas!"]
WAIT_EN = ["Keep going! I can see the difference!", "I'll wait out here, I don't want to get in the way.",
           "Is it looking better yet? I'm so nervous!", "The sponge gets everything off, even the cobwebs!"]


STORIES_FR = [
    ["Bonjour, qui que tu sois.\n\nJe m'appelle %1$s. Il y a quelques semaines, une terrible tempête a percé le toit et l'eau est entrée partout.",
     "Quand la pluie s'est enfin arrêtée, tout était couvert de boue et la moisissure a commencé à sortir des coins.\n\nJe ne savais pas par où commencer...",
     "Depuis, je passe mes journées au lit, sans envie de rien.\n\nMes éponges sont dans ce coffre. Si tu m'aides, je ne l'oublierai jamais.\n\n- %1$s"],
    ["Bonjour, voyageur.\n\nJe suis %1$s. J'ai passé une année entière chez mon cousin du désert. Que de sable, que de soleil !",
     "À mon retour, ma maison n'était plus la mienne : des toiles d'araignée, de la poussière jusqu'au plafond et des fenêtres si sales que la lumière n'entre plus.",
     "Je n'ai même plus la force de commencer.\n\nJe te laisse mes éponges ici. Tu me donnes un coup de main ?\n\n- %1$s"],
    ["Cher inconnu,\n\nJe suis %1$s. J'ai passé tout l'hiver au lit avec de la fièvre et de la soupe de betterave.",
     "Pendant ce temps, la maison s'est salie petit à petit. D'abord la poussière, puis les taches, puis... ce truc vert dans les coins.",
     "Je vais mieux, mais la voir comme ça me brise le cœur.\n\nSi tu as un moment, mes éponges sont dans ce coffre.\n\n- %1$s"],
    ["Salut.\n\nJe m'appelle %1$s. Un soir, j'ai oublié de ramoner la cheminée et... fouf ! Une fumée noire a envahi toute la maison.",
     "Rien n'a brûlé, heureusement, mais la suie a tout recouvert : murs, sol, fenêtres. Même mon chat est devenu gris.",
     "J'ai essayé de nettoyer moi-même, mais je n'ai fait qu'étaler la saleté.\n\nJe te laisse les éponges ici. S'il te plaît, aide-moi.\n\n- %1$s"],
    ["Bonjour, courageux.\n\nJe suis %1$s. Un soir, une araignée est entrée. Puis deux. Puis... j'ai perdu le compte.",
     "Elles sont parties avec le froid, mais ont laissé des toiles dans chaque coin, et tout est plein de poussière et de taches.",
     "Ça me rend si triste de voir ma maison ainsi que je ne sors même plus commercer.\n\nTout est dans ce coffre. L'éponge enlève même les toiles !\n\n- %1$s"],
    ["À qui trouvera ceci :\n\nJe suis %1$s. Des pillards ont attaqué le village et sont entrés chez moi avec leurs bottes pleines de boue.",
     "Ils ont cassé les lanternes, tout piétiné et laissé les murs dans un état lamentable. Maintenant il fait sombre et ça sent le marécage.",
     "Ils sont partis, mais je reste ici sans savoir quoi faire.\n\nSi tu pouvais m'aider à la remettre comme avant... les éponges sont dans le coffre.\n\n- %1$s"],
]
LETTERS_FR = [
    ["Cher/Chère %2$s,\n\nMa maison brille ! Plus une goutte de boue, et la moisissure est partie vivre ailleurs (loin, j'espère).",
     "Merci du fond du cœur. Il y aura toujours une place pour toi à ma table.\n\nAvec affection, et une odeur de savon,\n%1$s"],
    ["Cher/Chère %2$s,\n\nRegarde ces fenêtres ! Il entre tant de soleil qu'on se croirait au désert, mais sans le sable.",
     "Les araignées m'ont écrit pour se plaindre. Je leur ai dit que j'étais désolé (ce n'est pas vrai).\n\nMerci, vraiment.\n%1$s"],
    ["Cher/Chère %2$s,\n\nTu ne sais pas ce que ça représente de retrouver une maison propre après un si long hiver.",
     "J'ai retrouvé toute mon énergie. Passe quand tu veux pour une soupe de betterave. Sans fièvre, cette fois.\n\nAvec affection,\n%1$s"],
    ["Cher/Chère %2$s,\n\nAdieu la suie ! Les murs ont retrouvé leur couleur et mon chat est de nouveau orange.",
     "Je promets de ramoner la cheminée chaque semaine. Enfin, chaque mois. Enfin... j'essaierai.\n\nMille mercis,\n%1$s"],
    ["Cher/Chère %2$s,\n\nPlus une seule toile ! J'ai regardé trois fois sous le lit et tout brille.",
     "Tu es la personne la plus courageuse du village. Si les araignées reviennent, je sais qui appeler.\n\nJe t'embrasse,\n%1$s"],
    ["Cher/Chère %2$s,\n\nMa maison est de nouveau mon foyer. Les lanternes sont revenues, la lumière aussi, et même l'envie de chanter.",
     "Que les pillards tremblent : j'ai maintenant une amitié qui nettoie plus vite qu'ils ne salissent.\n\nMerci,\n%1$s"],
]
STORIES_DE = [
    ["Hallo, wer auch immer das liest.\n\nIch heiße %1$s. Vor ein paar Wochen hat ein schrecklicher Sturm das Dach durchbrochen und überall kam Wasser herein.",
     "Als der Regen endlich aufhörte, war alles voller Schlamm, und in den Ecken begann Schimmel zu wachsen.\n\nIch wusste nicht, wo ich anfangen sollte...",
     "Seitdem liege ich den ganzen Tag im Bett, ohne Lust auf irgendetwas.\n\nMeine Schwämme sind in dieser Truhe. Wenn du mir hilfst, vergesse ich das nie.\n\n- %1$s"],
    ["Hallo, Reisende(r).\n\nIch bin %1$s. Ich war ein ganzes Jahr bei meinem Cousin in der Wüste. So viel Sand, so viel Sonne!",
     "Als ich zurückkam, war mein Haus nicht mehr meins: Spinnweben, Staub bis zur Decke und Fenster so schmutzig, dass kein Licht hereinkommt.",
     "Mir fehlt sogar die Kraft, um anzufangen.\n\nIch lasse dir meine Schwämme hier. Hilfst du mir?\n\n- %1$s"],
    ["Liebe(r) Unbekannte(r),\n\nich bin %1$s. Ich lag den ganzen Winter mit Fieber und Rote-Bete-Suppe im Bett.",
     "Währenddessen wurde das Haus immer schmutziger. Erst Staub, dann Flecken, dann... dieses grüne Zeug in den Ecken.",
     "Jetzt geht es mir besser, aber es bricht mir das Herz, es so zu sehen.\n\nWenn du Zeit hast: Meine Schwämme sind in dieser Truhe.\n\n- %1$s"],
    ["Hallo.\n\nIch heiße %1$s. Eines Abends habe ich vergessen, den Kamin zu fegen, und... wusch! Schwarzer Rauch füllte das ganze Haus.",
     "Zum Glück ist nichts verbrannt, aber Ruß bedeckt alles: Wände, Boden, Fenster. Sogar meine Katze ist grau geworden.",
     "Ich habe versucht, selbst zu putzen, aber ich habe den Dreck nur verschmiert.\n\nDie Schwämme liegen hier. Bitte hilf mir.\n\n- %1$s"],
    ["Hallo, du Mutige(r).\n\nIch bin %1$s. Eines Nachts kam eine Spinne herein. Dann zwei. Dann... habe ich aufgehört zu zählen.",
     "Als es kalt wurde, sind sie gegangen, aber sie haben in jeder Ecke Spinnweben hinterlassen, und alles ist voller Staub und Flecken.",
     "Es macht mich so traurig, dass ich nicht einmal mehr zum Handeln rausgehe.\n\nAlles, was du brauchst, ist in der Truhe. Der Schwamm entfernt sogar Spinnweben!\n\n- %1$s"],
    ["An den, der das findet:\n\nIch bin %1$s. Plünderer haben das Dorf überfallen und sind mit schlammigen Stiefeln in mein Haus getrampelt.",
     "Sie haben die Laternen zerbrochen, alles zertrampelt und die Wände ruiniert. Jetzt ist es dunkel und riecht nach Sumpf.",
     "Sie sind weg, aber ich sitze hier und weiß nicht, was ich tun soll.\n\nWenn du mir helfen könntest, es wieder schön zu machen... die Schwämme sind in der Truhe.\n\n- %1$s"],
]
LETTERS_DE = [
    ["Liebe(r) %2$s,\n\nmein Haus glänzt! Kein Tropfen Schlamm mehr, und der Schimmel ist woanders hingezogen (hoffentlich weit weg).",
     "Danke von ganzem Herzen. An meinem Tisch ist immer ein Platz für dich.\n\nIn Liebe und mit Seifenduft,\n%1$s"],
    ["Liebe(r) %2$s,\n\nschau dir diese Fenster an! Es kommt so viel Sonne herein, als wäre ich wieder in der Wüste, nur ohne Sand.",
     "Die Spinnen haben mir einen Beschwerdebrief geschrieben. Ich habe gesagt, es tut mir leid (tut es nicht).\n\nDanke, wirklich.\n%1$s"],
    ["Liebe(r) %2$s,\n\ndu ahnst nicht, was es bedeutet, nach so einem langen Winter in ein sauberes Zuhause zurückzukehren.",
     "Ich habe wieder Energie. Komm vorbei, wann du willst, auf eine Rote-Bete-Suppe. Diesmal ohne Fieber.\n\nHerzlich,\n%1$s"],
    ["Liebe(r) %2$s,\n\nauf Wiedersehen, Ruß! Die Wände haben ihre Farbe zurück und meine Katze ist wieder orange.",
     "Ich verspreche, den Kamin jede Woche zu fegen. Na ja, jeden Monat. Na ja... ich versuche es.\n\nTausend Dank,\n%1$s"],
    ["Liebe(r) %2$s,\n\nkeine einzige Spinnwebe mehr! Ich habe dreimal unter dem Bett nachgesehen und alles glänzt.",
     "Du bist die mutigste Person im ganzen Dorf. Wenn die Spinnen zurückkommen, weiß ich, wen ich rufe.\n\nUmarmung,\n%1$s"],
    ["Liebe(r) %2$s,\n\nmein Haus ist wieder mein Zuhause. Die Laternen sind zurück, das Licht auch, sogar die Lust zu singen.",
     "Die Plünderer sollen zittern: Jetzt habe ich einen Freund, der schneller putzt, als sie Dreck machen.\n\nDanke,\n%1$s"],
]
STORIES_IT = [
    ["Ciao, chiunque tu sia.\n\nMi chiamo %1$s. Qualche settimana fa una tempesta terribile ha sfondato il tetto e l'acqua è entrata dappertutto.",
     "Quando finalmente ha smesso di piovere, era tutto pieno di fango e la muffa ha cominciato a uscire dagli angoli.\n\nNon sapevo da dove cominciare...",
     "Da allora passo le giornate a letto, senza voglia di niente.\n\nLe mie spugne sono in questo baule. Se mi aiuti, non lo dimenticherò mai.\n\n- %1$s"],
    ["Ciao, viaggiatore.\n\nSono %1$s. Ho passato un anno intero da mio cugino nel deserto. Quanta sabbia, quanto sole!",
     "Al ritorno la mia casa non era più mia: ragnatele, polvere fino al soffitto e finestre così sporche che la luce non entra più.",
     "Non ho nemmeno la forza di cominciare.\n\nTi lascio qui le mie spugne. Mi dai una mano?\n\n- %1$s"],
    ["Caro sconosciuto,\n\nsono %1$s. Ho passato tutto l'inverno a letto con la febbre e la zuppa di barbabietole.",
     "Nel frattempo la casa si è sporcata poco a poco. Prima la polvere, poi le macchie, poi... quella roba verde negli angoli.",
     "Ora sto meglio, ma vederla così mi spezza il cuore.\n\nSe hai un momento, le mie spugne sono in questo baule.\n\n- %1$s"],
    ["Ciao.\n\nMi chiamo %1$s. Una sera ho dimenticato di pulire il camino e... fuuum! Un fumo nero ha riempito tutta la casa.",
     "Per fortuna non è bruciato niente, ma la fuliggine ha coperto tutto: pareti, pavimento, finestre. Persino il mio gatto è diventato grigio.",
     "Ho provato a pulire da solo, ma ho solo sparso lo sporco.\n\nTi lascio qui le spugne. Ti prego, aiutami.\n\n- %1$s"],
    ["Ciao, coraggioso.\n\nSono %1$s. Una notte è entrato un ragno. Poi due. Poi... ho perso il conto.",
     "Se ne sono andati con il freddo, ma hanno lasciato ragnatele in ogni angolo, e tutto è pieno di polvere e macchie.",
     "Mi rende così triste vedere la mia casa così che non esco più nemmeno a commerciare.\n\nNel baule c'è tutto. La spugna toglie anche le ragnatele!\n\n- %1$s"],
    ["A chi troverà questo:\n\nsono %1$s. Dei predoni hanno assalito il villaggio e sono entrati in casa mia con gli stivali pieni di fango.",
     "Hanno rotto le lanterne, calpestato tutto e rovinato le pareti. Ora è buio e c'è odore di palude.",
     "Se ne sono andati, ma io resto qui senza sapere cosa fare.\n\nSe potessi aiutarmi a rimetterla com'era... le spugne sono nel baule.\n\n- %1$s"],
]
LETTERS_IT = [
    ["Caro/a %2$s,\n\nla mia casa brilla! Non c'è più una goccia di fango, e la muffa si è trasferita altrove (lontano, spero).",
     "Grazie di cuore. Alla mia tavola ci sarà sempre un posto per te.\n\nCon affetto, e profumo di sapone,\n%1$s"],
    ["Caro/a %2$s,\n\nguarda che finestre! Entra così tanto sole che sembra di essere tornati nel deserto, ma senza sabbia.",
     "I ragni mi hanno scritto per lamentarsi. Ho detto che mi dispiaceva (non è vero).\n\nGrazie, davvero.\n%1$s"],
    ["Caro/a %2$s,\n\nnon sai cosa significa tornare in una casa pulita dopo un inverno così lungo.",
     "Ho di nuovo energia. Passa quando vuoi per una zuppa di barbabietole. Stavolta senza febbre.\n\nCon affetto,\n%1$s"],
    ["Caro/a %2$s,\n\naddio fuliggine! Le pareti hanno di nuovo il loro colore e il mio gatto è tornato arancione.",
     "Prometto di pulire il camino ogni settimana. Beh, ogni mese. Beh... ci proverò.\n\nMille grazie,\n%1$s"],
    ["Caro/a %2$s,\n\nnemmeno una ragnatela! Ho guardato sotto il letto tre volte ed è tutto splendente.",
     "Sei la persona più coraggiosa del villaggio. Se tornano i ragni, so chi chiamare.\n\nUn abbraccio,\n%1$s"],
    ["Caro/a %2$s,\n\nla mia casa è di nuovo il mio rifugio. Sono tornate le lanterne, la luce e perfino la voglia di cantare.",
     "Tremino i predoni: ora ho un'amicizia che pulisce più in fretta di quanto loro sporchino.\n\nGrazie,\n%1$s"],
]
SAD_FR = ["Regarde dans quel état... je n'ai plus de forces.", "Avant, cette maison brillait, tu sais ?", "Hmm... je n'ai pas envie de commercer aujourd'hui.",
          "Je ne retrouve même plus mes lanternes.", "Désolé, je ne suis pas d'humeur...", "*soupir*", "Si seulement quelqu'un m'aidait à nettoyer...",
          "Tout ce qu'il faut est dans le coffre près du lit... mais je n'y arrive pas.", "Les taches ne partent pas toutes seules, hein ?", "Hmm... hmm..."]
SAD_DE = ["Sieh dir das an... ich habe keine Kraft mehr.", "Früher hat dieses Haus geglänzt, weißt du?", "Hmm... heute habe ich keine Lust zu handeln.",
          "Ich finde nicht mal meine Laternen.", "Tut mir leid, mir ist nicht danach...", "*seufz*", "Wenn mir nur jemand beim Putzen helfen würde...",
          "Alles, was ich brauche, ist in der Truhe am Bett... aber ich schaffe es nicht.", "Flecken gehen nicht von allein weg, oder?", "Hmm... hmm..."]
SAD_IT = ["Guarda com'è ridotta... non ho più forze.", "Prima questa casa brillava, sai?", "Hmm... oggi non ho voglia di commerciare.",
          "Non trovo nemmeno le mie lanterne.", "Scusa, non sono dell'umore...", "*sospiro*", "Se solo qualcuno mi aiutasse a pulire...",
          "Tutto quello che serve è nel baule accanto al letto... ma non ce la faccio.", "Le macchie non se ne vanno da sole, vero?", "Hmm... hmm..."]
WAIT_FR = ["Courage ! On voit la différence !", "J'attends dehors, je ne veux pas gêner.", "Ça avance ? Je suis si nerveux !", "L'éponge enlève tout, même les toiles !"]
WAIT_DE = ["Weiter so! Man sieht den Unterschied!", "Ich warte draußen, ich will nicht stören.", "Sieht es schon besser aus? Ich bin so aufgeregt!", "Der Schwamm schafft alles, sogar Spinnweben!"]
WAIT_IT = ["Forza! Si vede la differenza!", "Aspetto qui fuori, non voglio disturbare.", "Sta venendo bene? Che emozione!", "La spugna toglie tutto, anche le ragnatele!"]

UI = {
    # key: (en, es, fr, de, it)
    "item.cleanvillage.soapy_sponge": ("Soapy Sponge", "Esponja jabonosa", "Éponge savonneuse", "Seifenschwamm", "Spugna insaponata"),
    "item.cleanvillage.soapy_sponge.hint1": ("Hold right or left click on dirt to scrub it", "Mantén clic derecho o izquierdo sobre la suciedad para frotar",
                                          "Maintiens clic droit ou gauche sur la saleté pour frotter", "Halte Rechts- oder Linksklick auf Schmutz, um zu schrubben",
                                          "Tieni premuto il clic destro o sinistro sullo sporco per strofinare"),
    "item.cleanvillage.soapy_sponge.hint2": ("Right click on water to soak it again", "Clic derecho en el agua para volver a mojarla",
                                          "Clic droit sur l'eau pour la remouiller", "Rechtsklick auf Wasser, um ihn wieder nass zu machen",
                                          "Clic destro sull'acqua per bagnarla di nuovo"),
    "block.cleanvillage.family_portrait": ("Family Portrait", "Retrato de familia", "Portrait de famille", "Familienporträt", "Ritratto di famiglia"),
    "item.cleanvillage.family_portrait.lore": ("A gift from %s and family", "Un regalo de %s y su familia", "Un cadeau de %s et sa famille",
                                           "Ein Geschenk von %s und Familie", "Un regalo di %s e famiglia"),
    "block.cleanvillage.grime": ("Grime", "Mugre", "Crasse", "Schmutz", "Sporcizia"),
    "block.cleanvillage.mold": ("Mold", "Moho", "Moisissure", "Schimmel", "Muffa"),
    "block.cleanvillage.dusty_cobweb": ("Dusty Cobweb", "Telaraña polvorienta", "Toile poussiéreuse", "Staubige Spinnwebe", "Ragnatela polverosa"),
    "bossbar.cleanvillage.house": ("%s's house: %s%%", "Casa de %s: %s%%", "Maison de %s : %s%%", "Haus von %s: %s%%", "Casa di %s: %s%%"),
    "book.cleanvillage.story.title": ("What happened to my house", "Lo que le pasó a mi casa", "Ce qui est arrivé à ma maison",
                                  "Was mit meinem Haus passiert ist", "Cosa è successo alla mia casa"),
    "book.cleanvillage.letter.title": ("Thank-you letter from %s", "Carta de agradecimiento de %s", "Lettre de remerciement de %s",
                                   "Dankesbrief von %s", "Lettera di ringraziamento di %s"),
    "chat.cleanvillage.villager": ("<%s> %s",) * 5,
    "message.cleanvillage.sponge_dry": ("The sponge is dry: right click on water to soak it.", "La esponja está seca: haz clic derecho en el agua para mojarla.",
                                    "L'éponge est sèche : clic droit sur l'eau pour la mouiller.", "Der Schwamm ist trocken: Rechtsklick auf Wasser, um ihn nass zu machen.",
                                    "La spugna è asciutta: clic destro sull'acqua per bagnarla."),
    "message.cleanvillage.sponge_recharged": ("Sponge soaked and full of soap!", "¡Esponja mojada y llena de jabón!", "Éponge mouillée et pleine de savon !",
                                          "Schwamm nass und voller Seife!", "Spugna bagnata e piena di sapone!"),
    "message.cleanvillage.started": ("%s gets up and waits by the door.", "%s se levanta y sale a esperar a la puerta.", "%s se lève et attend près de la porte.",
                                 "%s steht auf und wartet an der Tür.", "%s si alza e aspetta alla porta."),
    "message.cleanvillage.halfway": ("%s peeks in: \"It looks so much better!\"", "%s se asoma: «¡Se ve mucho mejor!»", "%s jette un œil : « C'est tellement mieux ! »",
                                 "%s schaut herein: „Es sieht schon viel besser aus!“", "%s sbircia dentro: «Va molto meglio!»"),
    "message.cleanvillage.thanks": ("%s's house is cleanvillage! Read your letter.", "¡La casa de %s está reluciente! Mira tu carta.", "La maison de %s est impeccable ! Lis ta lettre.",
                                "Das Haus von %s ist blitzblank! Lies deinen Brief.", "La casa di %s è splendente! Leggi la tua lettera."),
    "subtitles.cleanvillage.scrub": ("Scrubbing", "Frotando", "Frottement", "Schrubben", "Strofinare"),
    "subtitles.cleanvillage.bubbles": ("Soap bubbles", "Burbujas de jabón", "Bulles de savon", "Seifenblasen", "Bolle di sapone"),
    "subtitles.cleanvillage.sparkle": ("Sparkle", "Brillo", "Scintillement", "Funkeln", "Scintillio"),
    "subtitles.cleanvillage.drip": ("Dripping", "Goteo", "Gouttes", "Tropfen", "Gocciolio"),
    "subtitles.cleanvillage.sponge_dry": ("Sponge dries out", "Esponja seca", "L'éponge sèche", "Schwamm trocknet aus", "La spugna si secca"),
    "itemGroup.cleanvillage": ("Clean Village",) * 5,
}
LANGS = ["en_us", "es_es", "fr_fr", "de_de", "it_it"]
DATA = {
    "en_us": (STORIES_EN, LETTERS_EN, SAD_EN, WAIT_EN),
    "es_es": (STORIES_ES, LETTERS_ES, SAD_ES, WAIT_ES),
    "fr_fr": (STORIES_FR, LETTERS_FR, SAD_FR, WAIT_FR),
    "de_de": (STORIES_DE, LETTERS_DE, SAD_DE, WAIT_DE),
    "it_it": (STORIES_IT, LETTERS_IT, SAD_IT, WAIT_IT),
}
DIRTY_WORD = {"en_us": ("Dirty ", ""), "fr_fr": ("", " (sale)"), "de_de": ("", " (schmutzig)"), "it_it": ("", " (sporco)")}


def dirty_name(lang, base):
    if lang == "es_es":
        return es_dirty(base)
    pre, post = DIRTY_WORD[lang]
    return pre + base + post


def build(lang):
    i = LANGS.index(lang)
    T = {k: v[i] for k, v in UI.items()}
    stories, letters, sad, wait = DATA[lang]
    for a, pages in enumerate(stories):
        for j, p in enumerate(pages):
            T[f"book.cleanvillage.story.{a}.page{j + 1}"] = p
    for a, pages in enumerate(letters):
        for j, p in enumerate(pages):
            T[f"book.cleanvillage.letter.{a}.page{j + 1}"] = p
    for a, x in enumerate(sad):
        T[f"villager.cleanvillage.sad.{a + 1}"] = x
    for a, x in enumerate(wait):
        T[f"villager.cleanvillage.waiting.{a + 1}"] = x
    return T


TEXTS = {l: build(l) for l in LANGS}
