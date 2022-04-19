package helloWorld;

import peersim.edsim.*;
import peersim.core.*;

import java.util.Random;

import peersim.config.*;

public class HelloWorld implements EDProtocol {

	//identifiant de la couche transport
	private int transportPid;

	//objet couche transport
	private HWTransport transport;

	//identifiant de la couche courante (la couche applicative)
	private int mypid;

	//le numero de noeud
	private int nodeId;

	//prefixe de la couche (nom de la variable de protocole du fichier de config)
	private String prefix;

	//on cr�� les voisins
	private Node voisin_d;
	private Node voisin_g;
	
	//bloqueur en cas de join
	private boolean chgt_en_cours = false;

	public HelloWorld(String prefix) {
		this.prefix = prefix;
		//initialisation des identifiants a partir du fichier de configuration
		this.transportPid = Configuration.getPid(prefix + ".transport");
		this.mypid = Configuration.getPid(prefix + ".myself");
		this.transport = null;
		this.voisin_d=null;
		this.voisin_g=null;
	}

	//methode appelee lorsqu'un message est recu par le protocole HelloWorld du noeud
	public void processEvent( Node node, int pid, Object event ) {
		this.receive((Message)event);
	}

	//methode necessaire pour la creation du reseau (qui se fait par clonage d'un prototype)
	public Object clone() {

		HelloWorld dolly = new HelloWorld(this.prefix);

		return dolly;
	}

	//liaison entre un objet de la couche applicative et un 
	//objet de la couche transport situes sur le meme noeud
	public void setTransportLayer(int nodeId) {
		this.nodeId = nodeId;
		this.transport = (HWTransport) Network.get(this.nodeId).getProtocol(this.transportPid);
	}
	
	//initialise pour effectuer des timeout
	public void init(Node me, int again, long time) {
		System.out.println("Le noeud "+ this.getMyId() +" a �t� initialis�");
		this.timeout(me, again, time);
	}
	
	public void timeout(Node me, int again, long time) {
		again=again-1;
		//le noeud d�cide d'agir de mani�re al�atoire
		int nb = CommonState.r.nextInt(3);
		if (nb==1) {
			//si un join est en cours, il faut �viter de relancer un leave sans faire une boucle infini donc on relance timeout 
			//pour que la proc�dure du join puisse se terminer
			if (this.getAction()) {
				this.timeout(me, again+1, System.currentTimeMillis());
			}
			//situation normal
			else {
				//si le noeud est dans le cercle il sort, sinon il effectue un join
				if (this.getVg()!=null) {
					this.leave();
				}
				else {
					Node vg = null;
					HelloWorld insert = null;
					//on prend un noeud au hasard pour le noeud gauche (autre que le noeud)
					boolean noeudg = true;
					while (noeudg) {
						nb = CommonState.r.nextInt(Network.size());
						vg = Network.get(nb);
						HelloWorld current = (HelloWorld)vg.getProtocol(this.mypid);
						if (current.getMyId()!=this.getMyId()) {
							noeudg=false;
						}
					}
					this.join(me,vg);
				}}
		}
		//on relance timeout au bout d'un certain temps
		if (again>0) {
			boolean relance=true;
			while (relance) {
				if (System.currentTimeMillis()>=time+30) {
					relance=false;
					this.timeout(me, again, System.currentTimeMillis());
				}
			}
		}
	}
	
	//envoi d'un message (l'envoi se fait via la couche transport)
	public void send(Message msg, Node dest) {
		this.transport.send(getMyNode(), dest, msg, this.mypid);
	}

	//affichage a la reception
	private void receive(Message msg) {
		
		//pour les messages � faire circuler
		if (msg.getTypeM()=="message") {
			while (getAction()) {
			}
			System.out.println(CommonState.getTime()+":"+ this + ": Received " + msg.getContent() +" and will send to "+this.getVd().getProtocol(this.mypid));
			if (this.getMyId()!=0) {
				this.send(msg, this.getVd());
			}
		}	
		//pour le message � destination inconnu
		if (msg.getTypeM()=="inconnu") {
			while (getAction()) {
			}
			boolean pas_moi=false;
			HelloWorld vd = (HelloWorld)this.getVd().getProtocol(this.mypid);
			//on regarde si le message est pour nous ou plus pour le voisin
			//ecart entre la valeur et le noeud
			long m=Math.abs((long)msg.getContent()-this.getMyId());
			//ecart entre la valeur et le noeud voisin droit
			long v=Math.abs((long)msg.getContent()-vd.getMyId());
			if (v<m) {
				pas_moi=true;
			}
			if (pas_moi) {
				//si le message n'est pas pour le noeud on transfert au voisin de droite
				System.out.println(CommonState.getTime()+":"+ this + ": Received message without destination and will send to "+this.getVd().getProtocol(this.mypid));
				if (vd.getMyId()!=0) {
					this.send(msg, this.getVd());
				}}
			else {
				//si le message est pour nous on le dit
				System.out.println(CommonState.getTime()+":"+ this + ": the unknown destination message is for me!");
			}
		}
		
		//pour les cas de join
		if (msg.getTypeM()=="joind") {
			//on bloque les potentiels messages arrivant pour ne pas transmettre le temps du changement de voisin
			this.setAction(false);
			if (this.getVd()!=null) {
				System.out.println("Acceptation insertion");
				//on envoie au noeud que nous acceptons de devenir voisin, et indiquons qui est son autre voisin
				Message m = new Message(Message.HELLOWORLD,this.getVd(),"okjoind");
				this.send(m,(Node)msg.getContent());
				//on pr�vient notre actuel noeud droit du changement en indiquant le noeud � ins�rer
				Message m2 = new Message(Message.HELLOWORLD,msg.getContent(),"joing");
				this.send(m2,this.getVd());
				//on met � jour le voisin droit
				this.setVd((Node)msg.getContent());
			}
			else {
				System.out.println("Refus insertion");
				//si pas d'insertion on se d�bloque
				this.setAction(true);
				//on envoie au noeud que nous ne sommes pas dans le cercle, et renvoie comme contenu le noeud qui a envoy� la demande
				Message m = new Message(Message.HELLOWORLD,msg.getContent(),"notjoin");
				this.send(m,(Node)msg.getContent());
			}
		}
		if (msg.getTypeM()=="joing") {
			//changement noeud gauche du node interne cercle
			this.setVg((Node)msg.getContent());
			//on pr�vient qu'on a chang� de voisin gauche
			Message m3 = new Message(Message.HELLOWORLD,msg.getContent(),"okjoing");
			this.send(m3,this.getVg());
		}
		if (msg.getTypeM()=="okjoind") {
			//changement noeud droit du node externe cercle
			this.setVd((Node)msg.getContent());
		}
		if (msg.getTypeM()=="okjoing") {
			//quand tout le monde a effectu� ses changements de voisins 
			//on d�bloque le noeud gauche pour les messages
			Message m4 = new Message(Message.HELLOWORLD,msg.getContent(),"ok join");
			this.send(m4,this.getVg());
			//et on se d�bloque pour pouvoir leave
			this.setAction(false);
		}
		if (msg.getTypeM()=="ok join") {
			//on d�bloque l'envoie de message quand les changements de voisins sont bien effectu�s
			this.setAction(false);
			System.out.println("Insertion effectu�e");
		}
		if (msg.getTypeM()=="notjoin") {
			System.out.println("relance insertion");
			Node vg = null, me = (Node)msg.getContent();
			HelloWorld current = null, insert = (HelloWorld)me.getProtocol(this.mypid);
			boolean noeudg = true;
			while (noeudg) {
				int nb = CommonState.r.nextInt(Network.size());
				vg = Network.get(nb);
				current = (HelloWorld)vg.getProtocol(this.mypid);
				if (current.getMyId()!=insert.getMyId()) {
					noeudg=false;
					this.join(me, vg);
				}
			}
		}
		//pour les cas de leave
		if (msg.getTypeM()=="leaved") {
			this.setVd((Node)msg.getContent());
		}
		if (msg.getTypeM()=="leaveg") {
			this.setVg((Node)msg.getContent());
		}
	}

	public void join(Node me, Node ng) {
		//on indique au noeud ng qu'on veut s'ins�rer � sa droite 
		//et qu'il devienne le noeud gauche du noeud
		this.setAction(true);
		System.out.println("Le node " + this.getMyId() + " veut s'ins�rer dans le cercle");
		Message mg = new Message(Message.HELLOWORLD,me,"joind");
		this.send(mg, ng);
		//on modifie et met en m�moire que ce noeud est le noeud gauche
		this.setVg(ng);
	}
	
	public void leave() {
		//on indique aux voisins qu'on part
		//et on leur indique leur nouveau noeud voisin
		System.out.println("Le node " + this.getMyId() + " veut sortir du cercle");
		Message mgg = new Message(Message.HELLOWORLD,this.getVd(),"leaved");
		this.send(mgg, this.getVg());
		Message mgd = new Message(Message.HELLOWORLD,this.getVg(),"leaveg");
		this.send(mgd, this.getVd());
		//on modifie nos propres voisins
		this.setVg(null);
		this.setVd(null);
	}

	//retourne le noeud courant
	private Node getMyNode() {
		return Network.get(this.nodeId);
	}

	public String toString() {
		return "Node "+ this.nodeId;
	}

	public int getMyId() {
		return this.nodeId;
	}

	public void setVg(Node n) {
		this.voisin_g=n;
	}

	public void setVd(Node n) {
		this.voisin_d=n;
	}

	public Node getVg() {
		return this.voisin_g;
	}
	public Node getVd() {
		return this.voisin_d;
	}
	public boolean getAction() {
		return chgt_en_cours;
	}
	public void setAction(boolean x) {
		this.chgt_en_cours=x;
	}
}