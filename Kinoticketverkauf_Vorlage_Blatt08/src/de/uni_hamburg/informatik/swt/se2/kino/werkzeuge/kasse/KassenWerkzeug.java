package de.uni_hamburg.informatik.swt.se2.kino.werkzeuge.kasse;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import de.uni_hamburg.informatik.swt.se2.kino.fachwerte.Datum;
import de.uni_hamburg.informatik.swt.se2.kino.materialien.Kino;
import de.uni_hamburg.informatik.swt.se2.kino.materialien.Tagesplan;
import de.uni_hamburg.informatik.swt.se2.kino.materialien.Vorstellung;
import de.uni_hamburg.informatik.swt.se2.kino.werkzeuge.SubwerkzeugObserver;
import de.uni_hamburg.informatik.swt.se2.kino.werkzeuge.bezahlvorgang.BezahlWerkzeug;
import de.uni_hamburg.informatik.swt.se2.kino.werkzeuge.datumsauswaehler.DatumAuswaehlWerkzeug;
import de.uni_hamburg.informatik.swt.se2.kino.werkzeuge.platzverkauf.PlatzVerkaufsWerkzeug;
import de.uni_hamburg.informatik.swt.se2.kino.werkzeuge.vorstellungsauswaehler.VorstellungsAuswaehlWerkzeug;

/**
 * Das Kassenwerkzeug. Mit diesem Werkzeug kann die Benutzerin oder der Benutzer
 * eine Vorstellung auswählen und Karten für diese Vorstellung verkaufen und
 * stornieren.
 * 
 * @author SE2-Team
 * @version SoSe 2016
 */
public class KassenWerkzeug
{

    // Das Material dieses Werkzeugs
    private Kino _kino;

    // UI dieses Werkzeugs
    private KassenWerkzeugUI _ui;

    // Die Subwerkzeuge
    private PlatzVerkaufsWerkzeug _platzVerkaufsWerkzeug;
    private DatumAuswaehlWerkzeug _datumAuswaehlWerkzeug;
    private VorstellungsAuswaehlWerkzeug _vorstellungAuswaehlWerkzeug;
	private BezahlWerkzeug _bezahlWerkzeug;
	

    /**
     * Initialisiert das Kassenwerkzeug.
     * 
     * @param kino das Kino, mit dem das Werkzeug arbeitet.
     * 
     * @require kino != null
     */
    public KassenWerkzeug(Kino kino)
    {
        assert kino != null : "Vorbedingung verletzt: kino != null";

        _kino = kino;

        // Subwerkzeuge erstellen
        _platzVerkaufsWerkzeug = new PlatzVerkaufsWerkzeug();
        _datumAuswaehlWerkzeug = new DatumAuswaehlWerkzeug();
        _vorstellungAuswaehlWerkzeug = new VorstellungsAuswaehlWerkzeug();
        _bezahlWerkzeug = new BezahlWerkzeug();

        erzeugeListenerFuerSubwerkzeuge();

        // UI erstellen (mit eingebetteten UIs der direkten Subwerkzeuge)
        _ui = new KassenWerkzeugUI(_platzVerkaufsWerkzeug.getUIPanel(),
                _datumAuswaehlWerkzeug.getUIPanel(),
                _vorstellungAuswaehlWerkzeug.getUIPanel());
        

        registriereUIAktionen();
        setzeTagesplanFuerAusgewaehltesDatum();
        setzeAusgewaehlteVorstellung();

        _ui.zeigeFenster();
    }

    /**
     * Erzeugt und registriert die Beobachter, die die Subwerkzeuge beobachten.
     */
    private void erzeugeListenerFuerSubwerkzeuge()
    {
        _datumAuswaehlWerkzeug.registriereBeobachter(new SubwerkzeugObserver()
        {
            @Override
            public void reagiereAufAenderung(String arg)
            {
                setzeTagesplanFuerAusgewaehltesDatum();
            }
        });

        _vorstellungAuswaehlWerkzeug
                .registriereBeobachter(new SubwerkzeugObserver()
                {
                    @Override
                    public void reagiereAufAenderung(String arg)
                    {
                        setzeAusgewaehlteVorstellung();
                    }
                });
        
        _platzVerkaufsWerkzeug
	        .registriereBeobachter(new SubwerkzeugObserver()
	        {
	            @Override
	            public void reagiereAufAenderung(String arg)
	            {
	            	if (arg.equals(PlatzVerkaufsWerkzeug.AKTION_AKTUALISIEREN))
	            	{
	            		aktualisiereBezahlFenster();
	            	}
	            	else if(arg.equals(PlatzVerkaufsWerkzeug.AKTION_VERKAUFEN))
	            	{
	            		oeffneBezahlFenster();
	            	}
	            }
	        });
        _bezahlWerkzeug
	        .registriereBeobachter(new SubwerkzeugObserver()
	        {
	            @Override
	            public void reagiereAufAenderung(String arg)
	            {
	            	if (arg.equals(BezahlWerkzeug.AKTION_VERKAUF))
	            	{
	            		_platzVerkaufsWerkzeug.fuehreBarzahlungDurch();
	            	}
	            	else if(arg.equals(BezahlWerkzeug.AKTION_ABBRUCH))
	            	{
	            		_platzVerkaufsWerkzeug.aktualisierePlatzplan();
	            	}
	            }
	        });
    }
    
    /**
     * Fügt die Funktionalitat zum Beenden-Button hinzu.
     */
    private void registriereUIAktionen()
    {
        _ui.getBeendenButton().addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                reagiereAufBeendenButton();
            }
        });
    }

    /**
     * Setzt den in diesem Werkzeug angezeigten Tagesplan basierend auf dem
     * derzeit im DatumsAuswahlWerkzeug ausgewählten Datum.
     */
    private void setzeTagesplanFuerAusgewaehltesDatum()
    {
        Tagesplan tagesplan = _kino.getTagesplan(getAusgewaehltesDatum());
        _vorstellungAuswaehlWerkzeug.setTagesplan(tagesplan);
    }

    /**
     * Passt die Anzeige an, wenn eine andere Vorstellung gewählt wurde.
     */
    private void setzeAusgewaehlteVorstellung()
    {
        _platzVerkaufsWerkzeug.setVorstellung(getAusgewaehlteVorstellung());
    }
    
    /**
     * Aktualisiert das Bezahlfenster.
     */
    private void aktualisiereBezahlFenster()
    {
    	_bezahlWerkzeug.aktualisierePreis(getAktuellenPreis());
    }
    
    /**
     * Öffnet das Bezahlfenster
     */
    private void oeffneBezahlFenster()
    {
    	_bezahlWerkzeug.showDialog();
    }
    

    /**
     * Beendet die Anwendung.
     */
    private void reagiereAufBeendenButton()
    {
        _ui.schliesseFenster();
        _bezahlWerkzeug.closeDialog();
    }
    
    private int getAktuellenPreis()
    {
    	return _platzVerkaufsWerkzeug.getAktuellenPreis();
    }

    /**
     * Gibt das derzeit gewählte Datum zurück.
     */
    private Datum getAusgewaehltesDatum()
    {
        return _datumAuswaehlWerkzeug.getSelektiertesDatum();
    }

    /**
     * Gibt die derzeit ausgewaehlte Vorstellung zurück. Wenn keine Vorstellung
     * ausgewählt ist, wird <code>null</code> zurückgegeben.
     */
    private Vorstellung getAusgewaehlteVorstellung()
    {
        return _vorstellungAuswaehlWerkzeug.getAusgewaehlteVorstellung();
    }
}
