package fr.svpro.caddyscan.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import fr.svpro.caddyscan.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerActivity extends AppCompatActivity {

    private static final String TAG = "ScannerActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    public static final String EXTRA_BARCODE = "extra_barcode";

    private PreviewView previewView;
    private TextView tvScanHint;
    private ExecutorService cameraExecutor;
    private boolean isScanComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        previewView = findViewById(R.id.previewView);
        tvScanHint = findViewById(R.id.tvScanHint);

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E,
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_DATA_MATRIX
                        ).build();

                BarcodeScanner scanner = BarcodeScanning.getClient(options);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (isScanComplete) {
                        imageProxy.close();
                        return;
                    }

                    @SuppressWarnings("UnsafeOptInUsageError")
                    InputImage image = InputImage.fromMediaImage(
                            imageProxy.getImage(),
                            imageProxy.getImageInfo().getRotationDegrees()
                    );

                    scanner.process(image)
                            .addOnSuccessListener(barcodes -> {
                                for (Barcode barcode : barcodes) {
                                    String rawValue = barcode.getRawValue();
                                    if (rawValue != null && !rawValue.isEmpty() && !isScanComplete) {
                                        isScanComplete = true;
                                        onBarcodeDetected(rawValue);
                                    }
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Barcode scan failed", e))
                            .addOnCompleteListener(task -> imageProxy.close());
                });

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera init failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void onBarcodeDetected(String barcode) {
        runOnUiThread(() -> {
            tvScanHint.setText("Code détecté : " + barcode);
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_BARCODE, barcode);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
