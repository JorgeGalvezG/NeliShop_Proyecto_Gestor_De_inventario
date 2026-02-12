import 'dart:io';
import 'package:flutter/material.dart';

class OptimizedImage extends StatelessWidget {
  final String? imagePath;
  final double? width;
  final double? height;
  final BoxFit fit;

  const OptimizedImage({
    super.key,
    this.imagePath,
    this.width,
    this.height,
    this.fit = BoxFit.cover,
  });

  @override
  Widget build(BuildContext context) {
    // Si no hay ruta o es vacía
    if (imagePath == null || imagePath!.isEmpty) {
      return _buildPlaceholder();
    }

    // Verificar si el archivo existe físicamente
    final file = File(imagePath!);
    if (!file.existsSync()) {
      return _buildPlaceholder();
    }

    return Image.file(
      file,
      width: width,
      height: height,
      fit: fit,
      cacheWidth: 500,
      errorBuilder: (context, error, stackTrace) => _buildPlaceholder(),
    );
  }

  Widget _buildPlaceholder() {
    return Container(
      width: width,
      height: height,
      color: Colors.grey[200],
      child: Icon(
        Icons.image_not_supported_outlined,
        color: Colors.grey[400],
        size: (width != null && width! < 50) ? 20 : 40,
      ),
    );
  }
}