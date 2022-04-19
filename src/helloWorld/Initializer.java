package helloWorld;

import peersim.edsim.*;
import peersim.core.*;

import java.util.Random;

import peersim.config.*;

/*
  Module d'initialisation de helloWorld: 
  Fonctionnement:
    pour chaque noeud, le module fait le lien entre la couche transport et la couche applicative ensuite, 
    selon les souhaits de simulation certaines lignes du code sont à mettr een commentaire ou enlever des commentaires, 
    pour ceci se référer au rapport du code
 */
public class Initializer implements peersim.core.Control {

	private int helloWorldPid;

	public Initializer(String prefix) {
		//recuperation du pid de la couche applicative
		this.helloWorldPid = Configuration.getPid(prefix + ".helloWorldProtocolPid");
	}

	public boolean execute() {
		int nodeNb;
		HelloWorld emitter, current = null, current2;
		Node dest;
		Node noeud;
		Message helloMsg, msg_inconnu;
		long nodedest;
		
		//recuperation de la taille du reseau
		nodeNb = Network.size();
		
		//creation du message
		helloMsg = new Message(Message.HELLOWORLD,"Hello !!!","message");
		if (nodeNb < 1) {
			System.err.println("Network size is not positive");
			System.exit(1);
		}
		
		//creation d'un message sans destination connue
		nodedest = CommonState.r.nextLong(nodeNb);
		//System.out.println("Le noeud de destination est le noeud se trouvant le plus proche (en id) du nombre : "+ nodedest);
		msg_inconnu = new Message(Message.HELLOWORLD,nodedest,"inconnu");

		//recuperation de la couche applicative de l'emetteur (le noeud 0)
		emitter = (HelloWorld)Network.get(0).getProtocol(this.helloWorldPid);
		emitter.setTransportLayer(0);

		//attribution des particularités à chaque noeud
		for ( int n=1 ; n<nodeNb; n++) {
			dest = Network.get(n);
			current = (HelloWorld)dest.getProtocol(this.helloWorldPid);
			current.setTransportLayer(n);
		}
		//attributions des voisins
		for ( int n=0 ; n<nodeNb; n++) {
			//noeud actuel
			noeud = Network.get(n);
			current2 = (HelloWorld)noeud.getProtocol(this.helloWorldPid);
			for (int i=0; i<nodeNb; i++) {
				//voisin
				dest = Network.get(i);
				current = (HelloWorld)dest.getProtocol(this.helloWorldPid);
				//attribution des voisins
				if (current.getMyId()==current2.getMyId()-1 || (current.getMyId()==nodeNb-1 && current2.getMyId()==0)) {
					current2.setVg(dest);
					current.setVd(noeud);
				}
			}
		}
		
		//on lance init sur le noeud 4
		int nbfois=5;
		Node node = Network.get(4);
		current = (HelloWorld)node.getProtocol(this.helloWorldPid);
		long timedep = System.currentTimeMillis();
		//current.init(node, nbfois, timedep);
		
		//on récupère le voisin du noeud 0 à qui il commence à envoyer ces messages
		current = (HelloWorld)emitter.getVd().getProtocol(this.helloWorldPid);
		
		//on fait envoyer au noeud 0 un message "Hello"
		emitter.send(helloMsg, emitter.getVd());
		
		//on envoie le message à destination inconu au noeud 0
		//emitter.send(msg_inconnu, Network.get(0));
		
		System.out.println("Initialization completed");
		return false;
	}
}