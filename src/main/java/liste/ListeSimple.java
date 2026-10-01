package liste;

import java.util.Objects;

public class ListeSimple {
    private long size;
    Noeud tete;

    public long getSize() {
        return size;
    }

    public void ajout(int element) {
        tete = new Noeud(element, tete);
        size++;
    }

    public void modifiePremier(Object element, Object nouvelleValeur) {
        Noeud courant = tete;
        while (courant != null && !Objects.equals(courant.getElement(), element))
            courant = courant.getSuivant();
        if (courant != null)
            courant.setElement(nouvelleValeur);
    }

    public void modifieTous(Object element, Object nouvelleValeur) {
        Noeud courant = tete;
        while (courant != null) {
            if (Objects.equals(courant.getElement(), element))
                courant.setElement(nouvelleValeur);
            courant = courant.getSuivant();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ListeSimple(");
        Noeud n = tete;
        while (n != null) {
            sb.append(n);
            n = n.getSuivant();
            if (n != null)
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    public void supprimePremier(Object element) {
        if (tete != null) {
            if (Objects.equals(tete.getElement(), element)) {
                tete = tete.getSuivant();
                size--;
                return;
            }
            Noeud precedent = tete;
            Noeud courant = tete.getSuivant();
            while (courant != null && !Objects.equals(courant.getElement(), element)) {
                precedent = precedent.getSuivant();
                courant = courant.getSuivant();
            }
            if (courant != null) {
                precedent.setSuivant(courant.getSuivant());
                size--;
            }
        }
    }

    public void supprimeTous(Object element) {
        tete = supprimeTousRecurs(element, tete);
    }

    public Noeud supprimeTousRecurs(Object element, Noeud tete) {
        if (tete != null) {
            Noeud suiteListe = supprimeTousRecurs(element, tete.getSuivant());
            if (Objects.equals(tete.getElement(), element)) {
                size--;
                return suiteListe;
            } else {
                tete.setSuivant(suiteListe);
                return tete;
            }
        } else return null;
    }

    public Noeud getAvantDernier() {
        if (tete == null || tete.getSuivant() == null)
            return null;
        else {
            Noeud courant = tete;
            Noeud suivant = courant.getSuivant();
            while (suivant.getSuivant() != null) {
                courant = suivant;
                suivant = suivant.getSuivant();
            }
            return courant;
        }
    }

    public void inverser() {
        Noeud precedent = null;
        Noeud courant = tete;
        while (courant != null) {
            Noeud next = courant.getSuivant();
            courant.setSuivant(precedent);
            precedent = courant;
            courant = next;
        }
        tete = precedent;
    }

    public Noeud getPrecedent(Noeud r) {
        if (r == null || tete == null || tete == r)
            return null;

        Noeud precedent = tete;
        Noeud courant = tete.getSuivant();
        while (courant != null && courant != r) {
            precedent = courant;
            courant = courant.getSuivant();
        }
        return courant == r ? precedent : null;
    }

    public void echanger(Noeud r1, Noeud r2) {
        if (r1 == r2)
            return;

        Noeud precedentR1 = getPrecedent(r1);
        Noeud precedentR2 = getPrecedent(r2);
        if ((r1 != tete && precedentR1 == null) || (r2 != tete && precedentR2 == null))
            throw new IllegalArgumentException("Les deux noeuds doivent appartenir à la liste");

        Noeud suivantR1 = r1.getSuivant();
        Noeud suivantR2 = r2.getSuivant();
        if (precedentR1 == r2) {
            if (precedentR2 != null)
                precedentR2.setSuivant(r1);
            else
                tete = r1;
            r1.setSuivant(r2);
            r2.setSuivant(suivantR1);
        } else if (precedentR2 == r1) {
            if (precedentR1 != null)
                precedentR1.setSuivant(r2);
            else
                tete = r2;
            r2.setSuivant(r1);
            r1.setSuivant(suivantR2);
        } else {
            if (precedentR1 != null)
                precedentR1.setSuivant(r2);
            else
                tete = r2;
            if (precedentR2 != null)
                precedentR2.setSuivant(r1);
            else
                tete = r1;
            r1.setSuivant(suivantR2);
            r2.setSuivant(suivantR1);
        }
    }

}