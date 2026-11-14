package com.payroll;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class InitialSetupTest {

    @Test
    @DisplayName("1. Verify running Java runtime is strictly Java 21")
    void testJava21Environment() {
        int featureVersion = Runtime.version().feature();
        assertEquals(21, featureVersion, "Application must run on Java 21 LTS. Actual feature version: " + featureVersion);
        
        String versionString = System.getProperty("java.version");
        assertNotNull(versionString, "java.version system property must not be null");
        assertTrue(versionString.startsWith("21"), "java.version string must start with '21'. Found: " + versionString);
    }

    @Test
    @DisplayName("2. Verify Apache PDFBox 3.0.2 document creation, text rendering, serialization and reloading")
    void testPdfBoxLifecycleAndIntegrity() throws IOException {
        byte[] pdfBytes;
        
        // Phase A: Create PDF, write content, and save to byte stream
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                contentStream.beginText();
                contentStream.setFont(font, 14);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Employee Payroll Management System - Stage 1 Verification");
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            pdfBytes = baos.toByteArray();
        }

        assertNotNull(pdfBytes, "PDF byte array must not be null");
        assertTrue(pdfBytes.length > 0, "PDF byte array must have non-zero length");

        // Phase B: Reload saved PDF using PDFBox 3.x Loader and verify internal page structure
        try (PDDocument reloadedDocument = Loader.loadPDF(pdfBytes)) {
            assertEquals(1, reloadedDocument.getNumberOfPages(), "Reloaded PDF must contain exactly 1 page");
            assertNotNull(reloadedDocument.getPage(0), "First page of reloaded PDF must exist");
        }
    }

    @Test
    @DisplayName("3 & 4. Verify JavaFX Toolkit startup, platform environment and FXML loading on FX thread")
    void testJavaFxToolkitAndFxThreadExecution() throws InterruptedException {
        CountDownLatch startupLatch = new CountDownLatch(1);
        AtomicReference<Throwable> startupError = new AtomicReference<>();

        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                startupLatch.countDown();
            });
        } catch (IllegalStateException e) {
            // Toolkit was already started in this JVM session
            Platform.setImplicitExit(false);
            startupLatch.countDown();
        } catch (Throwable t) {
            startupError.set(t);
        }

        assertNull(startupError.get(), "JavaFX Platform.startup threw unexpected error: " + startupError.get());
        assertTrue(startupLatch.await(5, TimeUnit.SECONDS), "JavaFX Platform startup must complete within 5 seconds");

        // Diagnostic environment reporting
        boolean headless = GraphicsEnvironment.isHeadless();
        String glassPlatform = System.getProperty("glass.platform", "default-native");
        System.out.println("[DIAGNOSTIC] AWT GraphicsEnvironment.isHeadless() = " + headless);
        System.out.println("[DIAGNOSTIC] JavaFX glass.platform = " + glassPlatform);

        // Execute node creation and FXML parsing directly on the JavaFX Application Thread
        CountDownLatch fxActionLatch = new CountDownLatch(1);
        AtomicBoolean uiActionSuccess = new AtomicBoolean(false);
        AtomicReference<Throwable> fxActionError = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                // Verify JavaFX control instantiation
                Label testLabel = new Label("Payroll System UI Test");
                assertEquals("Payroll System UI Test", testLabel.getText());

                // Verify loading of login.fxml
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/payroll/login.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "Root element from login.fxml must not be null");

                uiActionSuccess.set(true);
            } catch (Throwable t) {
                fxActionError.set(t);
            } finally {
                fxActionLatch.countDown();
            }
        });

        assertTrue(fxActionLatch.await(5, TimeUnit.SECONDS), "JavaFX Application thread execution timed out");
        assertNull(fxActionError.get(), "Error during JavaFX Application thread execution: " + fxActionError.get());
        assertTrue(uiActionSuccess.get(), "JavaFX UI nodes and login.fxml must load successfully on the FX thread");
    }

    @Test
    @DisplayName("5. Verify MainApp application launch, Scene setup, and Stage visibility")
    void testMainAppLaunchAndStageVisibility() throws InterruptedException {
        CountDownLatch startupLatch = new CountDownLatch(1);
        try {
            Platform.startup(startupLatch::countDown);
        } catch (IllegalStateException e) {
            startupLatch.countDown();
        }
        assertTrue(startupLatch.await(5, TimeUnit.SECONDS));

        CountDownLatch stageLatch = new CountDownLatch(1);
        AtomicBoolean stageShown = new AtomicBoolean(false);
        AtomicReference<String> stageTitle = new AtomicReference<>();
        AtomicReference<Throwable> stageError = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                Platform.setImplicitExit(false);
                Stage testStage = new Stage();
                MainApp app = new MainApp();
                app.start(testStage);
                stageShown.set(testStage.isShowing());
                stageTitle.set(testStage.getTitle());
                // Cleanly close the stage after verification
                testStage.close();
            } catch (Throwable t) {
                stageError.set(t);
            } finally {
                stageLatch.countDown();
            }
        });

        assertTrue(stageLatch.await(5, TimeUnit.SECONDS), "MainApp stage launch timed out");
        assertNull(stageError.get(), "Error during MainApp launch: " + stageError.get());
        assertTrue(stageShown.get(), "MainApp window stage must have been shown (isShowing == true)");
        assertEquals("Employee Payroll Management System - Login", stageTitle.get());
    }
}
