package br.com.padaria.util;
    
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.UIManager;

//APLICA O VISUAL MODERNO FLATLAF
public class Tema {
    
    private Tema() {
        
    }
    public static void aplicar() {
        FlatLightLaf.setup();                          // tema claro moderno
        UIManager.put("Button.arc", 10);               // botões arredondados
        UIManager.put("Component.arc", 10);            // combos arredondados
        UIManager.put("TextComponent.arc", 10);        // campos de texto arredondados
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.rowHeight", 26);          // linhas da tabela mais altas
        UIManager.put("ScrollBar.showButtons", false);
    }
}
