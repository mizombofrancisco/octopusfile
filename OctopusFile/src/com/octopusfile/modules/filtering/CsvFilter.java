package com.octopusfile.modules.filtering;

import java.util.function.Predicate;

/**
 * Configura como uma leitura de CSV deve ser interpretada (delimitador)
 * e, opcionalmente, filtra linhas já convertidas em colunas.
 * <p>
 * Diferente de {@link FileFilter} (que filtra {@code Path}s), este filtro
 * atua sobre o conteúdo já lido de um arquivo CSV, no formato
 * {@code String[]} (uma linha, já dividida em colunas).
 */
public class CsvFilter implements Predicate<String[]> {

    private String delimitador = ",";
    private Predicate<String[]> condicao = linha -> true;

    /** Define o delimitador usado para separar as colunas (padrão: ","). */
    public CsvFilter withDelimiter(String delimitador) {
        this.delimitador = delimitador;
        return this;
    }

    /** Define uma condição adicional para aceitar/rejeitar uma linha. */
    public CsvFilter withCondition(Predicate<String[]> condicao) {
        this.condicao = condicao;
        return this;
    }

    public String getDelimiter() {
        return delimitador;
    }

    @Override
    public boolean test(String[] linha) {
        return condicao.test(linha);
    }
}
