package com.ceng.cloud;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import server.droporchoose.UploadComponent;

@SpringUI
public class MainUI extends UI {

	private Image image;
	private ComboBox<COLOR> cbBackground;
	private ComboBox<COLOR> cbCodeColor;
	private File file;

	@Override
	protected void init(VaadinRequest request) {

		VerticalLayout imageLayout = new VerticalLayout();
		HorizontalLayout mainLayout = new HorizontalLayout();
		VerticalLayout readLayout = new VerticalLayout();

		UploadComponent upload = new UploadComponent();
		upload.setReceivedCallback(this::uploadReceived);

		Button btnRead = new Button("Read QR");
		btnRead.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				Window window = new Window("QR Data");

				try {
					if (file == null || file.length() == 0) {
						Notification.show("Please upload a QR file");
						return;
					}
					String data = decodeQRCode(file);
					if (data == null) {
						return;
					}
					Label lblData = new Label(data);

					window.setContent(lblData);
					window.center();
					window.setModal(true);

					getUI().addWindow(window);

					java.nio.file.Files.delete(file.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		});

		readLayout.addComponents(upload, btnRead);

		TextField tfName = new TextField("Name");
		cbBackground = new ComboBox<>("Background Color");
		cbBackground.setItems(COLOR.values());
		cbBackground.setItemCaptionGenerator(color -> color.getName());
		cbBackground.setValue(COLOR.WHITE);
		cbBackground.setEmptySelectionAllowed(false);

		cbCodeColor = new ComboBox<>("Code Color");
		cbCodeColor.setItems(COLOR.values());
		cbCodeColor.setItemCaptionGenerator(color -> color.getName());
		cbCodeColor.setValue(COLOR.BLACK);
		cbCodeColor.setEmptySelectionAllowed(false);

		Button btnCreate = new Button("Create");
		btnCreate.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				try {
					image = convertToImage(getQRCodeImage(tfName.getValue(), cbCodeColor.getValue().getColorId(),
							cbBackground.getValue().getColorId()));
					imageLayout.removeAllComponents();
					imageLayout.addComponent(image);
				} catch (WriterException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		});

		VerticalLayout inputLayout = new VerticalLayout();

		HorizontalLayout createLayout = new HorizontalLayout();

		inputLayout.addComponents(tfName, cbCodeColor, cbBackground, btnCreate);

		createLayout.addComponents(inputLayout, imageLayout);

		mainLayout.addComponents(createLayout, readLayout);

		setContent(mainLayout);

	}

	private byte[] getQRCodeImage(String text, int codeColor, int backgroundColor) throws WriterException, IOException {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 150, 150);

		MatrixToImageConfig conf = new MatrixToImageConfig(codeColor, backgroundColor);
		BufferedImage qrcode = MatrixToImageWriter.toBufferedImage(bitMatrix, conf);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(qrcode, "jpg", baos);
		baos.flush();
		byte[] imageInByte = baos.toByteArray();
		baos.close();

		return imageInByte;
	}

	private static Image convertToImage(final byte[] imageData) {
		StreamSource streamSource = new StreamResource.StreamSource() {
			public InputStream getStream() {
				return (imageData == null) ? null : new ByteArrayInputStream(imageData);
			}
		};

		return new Image(null, new StreamResource(streamSource, "streamedSourceFromByteArray"));
	}

	private static String decodeQRCode(File qrCodeimage) throws IOException {
		BufferedImage bufferedImage = ImageIO.read(qrCodeimage);
		LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

		try {
			Result result = new MultiFormatReader().decode(bitmap);
			return result.getText();
		} catch (NotFoundException e) {
			Notification.show("Image you provided is not a QR image");
			return null;
		}
	}

	private void uploadReceived(String fileName, Path path) {
		file = path.toFile();
	}

	@WebServlet(value = "/*", asyncSupported = true)
	@VaadinServletConfiguration(productionMode = true, ui = MainUI.class, heartbeatInterval = 30, closeIdleSessions = false)
	public static class Servlet extends VaadinServlet {

	}

}

enum COLOR {

	WHITE("White", 0xFFFFFFFF), BLACK("Black", 0xFF000000), RED("Red", 0xFFFF0000), GREEN("Green", 0xFF00FF00),
	BLUE("Blue", 0xFF0000FF);

	private String name;
	private int colorId;

	COLOR(String name, int colorId) {
		this.name = name;
		this.colorId = colorId;
	}

	String getName() {
		return this.name;
	}

	int getColorId() {
		return this.colorId;
	}
}
