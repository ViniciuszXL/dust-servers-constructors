package br.com.vinicius;

import java.awt.EventQueue;

import br.com.vinicius.view.ConstructorView;

public class Main {

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			ConstructorView view = new ConstructorView();
			try {
				view.create();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

}
