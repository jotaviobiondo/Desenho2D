package controle;

import controle.ferramentas.Ferramenta;
import controle.ferramentas.FerramentaEscala;
import formas.Circulo;
import formas.Forma;
import formas.Linha;
import formas.Retangulo;
import gui.PainelDesenho;
import gui.TelaPrincipal;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Timer;

public class ControleDesenho implements ActionListener {
    
    private final TelaPrincipal tela;
    private final PainelDesenho painelDesenho;
    private final MouseInput mouseInput;
    private final TecladoInput tecladoInput;
    
    private int mouseX;
    private int mouseY;
    private boolean desenhando;
    
    private boolean selecionando;
    private Retangulo rectSelecao;
    
    private boolean panning;
    private int mouseX_anterior;
    private int mouseY_anterior;
    
    private ArrayList<Forma> formas;
    private Forma formaDesenhando;
    
    private Ferramenta ferramenta;
    
    public static final int PROXIMIDADE = 15;
    private Point2D pontoProximidade;
    
    public static final double FATOR_ZOOM_IN = 1.5;
    public static final double FATOR_ZOOM_OUT = 0.66666667;
    public static final double ZOOM_MAX = 20;
    private double zoomAcc;
    
    private boolean modoOrtho;
    
    private final Timer timer;
    public static final int DELAY = 17;

    public ControleDesenho() {
        formas = new ArrayList<>();
        mouseInput = new MouseInput(this);
        tecladoInput = new TecladoInput(this);
        painelDesenho = new PainelDesenho(this);
        tela = new TelaPrincipal(this, painelDesenho);
        zoomAcc = 1;
        
        timer = new Timer(DELAY, this);
        timer.start();
    }
    
    private void atualizar(){
        //Snap
        pontoProximidade = null;
        for (Forma forma : formas) {
            forma.atualizar(mouseX, mouseY);
            
            if (rectSelecao != null && forma.intersecao(rectSelecao.getRect2D())){
                forma.setSelecionada(true);
            } else {
                forma.setSelecionada(false);
            }
            
            for (Point2D ponto : forma.getPontos()) {
                if (getRectProximidade(ponto.getX(), ponto.getY()).contains(mouseX, mouseY)) {
                    pontoProximidade = ponto;
                    break;
                }
            }
        }
        if (pontoProximidade != null){
            mouseX = (int) pontoProximidade.getX();
            mouseY = (int) pontoProximidade.getY();
        }
        
        if (desenhando){
            formaDesenhando.setDistancia(mouseX, mouseY, modoOrtho);
        }
        
        tela.atualizarGUI();
        painelDesenho.atualizar();
    }
    
    public void desenhar(Graphics2D g){
        g.setColor(Color.WHITE);
        
        if (desenhando){
            formaDesenhando.desenhar(g);
        }
        
        for (Forma forma : formas) {
            forma.desenhar(g);
        }
        
        if (pontoProximidade != null){
            g.setColor(Color.GREEN);
            g.draw(getRectProximidade(pontoProximidade.getX(), pontoProximidade.getY()));
        }
        
        if (selecionando){
            desenharSelecao(g);
        }
    }
    
    private void desenharSelecao(Graphics2D g){
        Color c1 = Color.GREEN.darker();
        Color c2 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), 50);
        
        rectSelecao.setCor(c2);
        rectSelecao.setFill(true);
        rectSelecao.desenhar(g);
        
        Stroke original = g.getStroke();
        float dash[] = { 10.0f };
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
        
        rectSelecao.setCor(Color.WHITE);
        rectSelecao.setFill(false);
        rectSelecao.desenhar(g);
        g.setStroke(original);
    }
    
    private void zoom(double zoom, int posX, int posY){
        if (zoomAcc*zoom > ZOOM_MAX){
            tela.mostrarMensagem("Zoom in máximo atingido.");
            return;
        }
        if (zoomAcc*zoom < 1/ZOOM_MAX){
            tela.mostrarMensagem("Zoom out máximo atingido.");
            return;
        }
        
        zoomAcc *= zoom;
        
        Point2D.Double referencia = new Point2D.Double(posX, posY);
        for (Forma forma : formas) {
            forma.escala(zoom, zoom, referencia);
        }
        if (desenhando){
            formaDesenhando.escala(zoom, zoom, referencia);
        }
        if (rectSelecao != null){
            rectSelecao.escala(zoom, zoom, referencia);
        }
    }
    
    public void zoomIn(int posX, int posY){
        zoom(FATOR_ZOOM_IN, posX, posY);
    }
    
    public void zoomIn(){
        zoomIn(painelDesenho.getWidth()/2, painelDesenho.getHeight()/2);
    }
    
    public void zoomOut(int posX, int posY){
        zoom(FATOR_ZOOM_OUT, posX, posY);
    }
    
    public void zoomOut(){
        zoomOut(painelDesenho.getWidth()/2, painelDesenho.getHeight()/2);
    }
    
    public void zoomExtend(){
        
    }
    
    public void pan(){
        int dx = mouseX - mouseX_anterior;
        int dy = mouseY - mouseY_anterior;
        
        for (Forma forma : formas) {
            forma.translacao(dx, dy);
        }
        if (desenhando){
            formaDesenhando.translacao(dx, dy);
        }
        if (rectSelecao != null){
            rectSelecao.translacao(dx, dy);
        }
    }
    
    public void cancelarForma(){
        desenhando = false;
        formaDesenhando = null;
        rectSelecao = null;
    }
    
    public void deletarSelecionado(){
        for (int i = formas.size()-1; i >= 0; i--) {
            if (formas.get(i).estaSelecionada()){
                formas.remove(i);
            }
        }
        rectSelecao = null;
    }
    
    public void limpar(){
        formas = new ArrayList<>();
        rectSelecao = null;
    }
    
    public void ativarModoOrtho(){
        modoOrtho = true;
    }
    
    public void desativarModoOrtho(){
        modoOrtho = false;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        atualizar();
    }
    
    public void ferramentaMover(){
        if (rectSelecao == null){
            tela.mostrarMensagem("Não há objetos selecionados.");
            return;
        }
        
        JTextField dxField = new JTextField();
        JTextField dyField = new JTextField();
        dxField.setText("0");
        dyField.setText("0");
        Object[] message = {
            "Deslocamento em X:", dxField,
            "Deslocamento em Y:", dyField
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Mover", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            double dx = Double.valueOf(dxField.getText());
            double dy = Double.valueOf(dyField.getText());
            
            for (Forma forma : getSelecionados()) {
                forma.translacao(dx, dy);
            }
        }
    }
    
    public void ferramentaEscala(){
        if (rectSelecao == null){
            tela.mostrarMensagem("Não há objetos selecionados.");
            return;
        }
        
        JTextField xRefField = new JTextField();
        JTextField yRefField = new JTextField();
        JTextField sxField = new JTextField();
        JTextField syField = new JTextField();
        sxField.setText("0");
        syField.setText("0");
        Object[] message = {
            "X Referência:", xRefField,
            "Y Referência:", yRefField,
            "Mudança de Escala em X:", sxField,
            "Mudança de Escala em Y:", syField
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Escala", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            double xRef = Double.valueOf(xRefField.getText());
            double yRef = Double.valueOf(yRefField.getText());
            double sx = Double.valueOf(sxField.getText());
            double sy = Double.valueOf(syField.getText());
            
            Point2D.Double referencia = new Point2D.Double(xRef, yRef);
            
            for (Forma forma : getSelecionados()) {
                forma.escala(sx, sy, referencia);
            }
        }
    }
    
    public void ferramentaRotacao(){
        if (rectSelecao == null){
            tela.mostrarMensagem("Não há objetos selecionados.");
            return;
        }
        
        JTextField xRefField = new JTextField();
        JTextField yRefField = new JTextField();
        JTextField alphaField = new JTextField();
        alphaField.setText("0");
        Object[] message = {
            "X Referência:", xRefField,
            "Y Referência:", yRefField,
            "Ângulo de Rotacao (em graus):", alphaField
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Rotação", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            double xRef = Double.valueOf(xRefField.getText());
            double yRef = Double.valueOf(yRefField.getText());
            double alpha = Double.valueOf(alphaField.getText());
            Point2D.Double referencia = new Point2D.Double(xRef, yRef);
            for (Forma forma : getSelecionados()) {
                forma.rotacao(alpha, referencia);
            }
        }
    }
    
    public ArrayList<Forma> getSelecionados(){
        ArrayList<Forma> selecionados = new ArrayList<>();
        for (Forma forma : formas) {
            if (forma.estaSelecionada()){
                selecionados.add(forma);
            }
        }
        return selecionados;
    }
    
    public void mostrarMensagem(String msg){
        tela.mostrarMensagem(msg);
    }
    
    private void criarForma() {
        rectSelecao = null;
        if (!desenhando) {
            switch (tela.getFerramentaSelecionada()){
                case "LINHA":
                    formaDesenhando = new Linha(mouseX, mouseY, mouseX, mouseY);
                    break;
                case "RETANGULO":
                    formaDesenhando = new Retangulo(mouseX, mouseY, 1, 1);
                    break;
                case "ELIPSE":
                    formaDesenhando = new Circulo(mouseX, mouseY, mouseX, mouseY);
                    break;
            }
        } else {
            formas.add(formaDesenhando);
        }
        desenhando = !desenhando;
    }
    
    private void ferramentaTransformacao(){
        /*if (rectSelecao == null){
            mostrarMensagem("Não há objetos selecionados.");
            return;
        }
        
        if (ferramenta == null){
            switch (tela.getFerramentaSelecionada()) {
                case "MOVER":
                    break;
                case "ESCALA":
                    ferramenta = new FerramentaEscala(this);
                    break;
                case "ROTACAO":
                    break;
            }
        }
        
        ferramenta.click(mouseX, mouseY);*/
    }
    
    private void acaoFerramenta(){
        switch (tela.getFerramentaSelecionada()) {
            case "LINHA":
            case "RETANGULO":
            case "ELIPSE":
                criarForma();
                break;
            case "MOVER":
            case "ESCALA":
            case "ROTACAO":
                ferramentaTransformacao();
                break;
        }
    }
    
    public void mouseClick(int posX, int posY, int botao) {
        switch (botao) {
            case MouseInput.BOTAO_ESQUERDO:
                acaoFerramenta();
                break;
            case MouseInput.BOTAO_MEIO:
                panning = true;
                mouseX_anterior = posX;
                mouseY_anterior = posY;
                painelDesenho.setPan(panning);
                break;
            case MouseInput.BOTAO_DIREITO:
                selecionando = true;
                rectSelecao = new Retangulo(mouseX, mouseY, 1, 1);
                break;
        }
    }
    
    public void mouseRelease(int posX, int posY, int botao){
        if (botao == MouseInput.BOTAO_MEIO){
            panning = false;
            painelDesenho.setPan(panning);
        } else if (botao == MouseInput.BOTAO_DIREITO){
            selecionando = false;
        }
    }
    
    public void mouseArrastar(int posX, int posY){
        mouseX = posX;
        mouseY = posY;
        if (panning){
            pan();
            mouseX_anterior = mouseX;
            mouseY_anterior = mouseY;
        } else if (selecionando){
            rectSelecao.setDistancia(posX, posY, modoOrtho);
        }
    }
    
    public void moverMouse(int x, int y){
        mouseX = x;
        mouseY = y;
    }
    
    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }
    
    public Rectangle2D getRectProximidade(double x, double y){
        return new Rectangle2D.Double(x - PROXIMIDADE/2, y - PROXIMIDADE/2, PROXIMIDADE, PROXIMIDADE);
    }

    public boolean isDesenhando() {
        return desenhando;
    }

    public Point2D getPontoProximidade() {
        return pontoProximidade;
    }

    public MouseInput getMouseInput() {
        return mouseInput;
    }

    public TecladoInput getTecladoInput() {
        return tecladoInput;
    }

    public boolean isPanning() {
        return panning;
    }
    
    public double getZoomAcc(){
        return zoomAcc;
    }

}
