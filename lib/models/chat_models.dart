import 'package:flutter/material.dart';

class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.text,
    required this.isUser,
  });

  final int id;
  final String text;
  final bool isUser;
}

enum ToolOption {
  camera(
    label: 'Camera',
    sendPrefix: 'Camera',
    sheetTitle: 'Camera',
    subtitle: 'Capture something new',
    icon: Icons.photo_camera_outlined,
  ),
  photos(
    label: 'Photos',
    sendPrefix: 'Photos',
    sheetTitle: 'Photos',
    subtitle: 'Use images from your gallery',
    icon: Icons.auto_awesome_rounded,
  ),
  files(
    label: 'Files',
    sendPrefix: 'Files',
    sheetTitle: 'Files',
    subtitle: 'Attach a document or note',
    icon: Icons.attach_file_rounded,
  ),
  web(
    label: 'Search',
    sendPrefix: 'Web search',
    sheetTitle: 'Web search',
    subtitle: 'Find real-time news and info',
    icon: Icons.language_rounded,
  ),
  image(
    label: 'Image',
    sendPrefix: 'Create image',
    sheetTitle: 'Create image',
    subtitle: 'Visualize anything',
    icon: Icons.image_search_rounded,
  ),
  mcp(
    label: 'Tools',
    sendPrefix: 'MCP Tools',
    sheetTitle: 'MCP Tools',
    subtitle: 'Connect external tools',
    icon: Icons.code_rounded,
  );

  const ToolOption({
    required this.label,
    required this.sendPrefix,
    required this.sheetTitle,
    required this.subtitle,
    required this.icon,
  });

  final String label;
  final String sendPrefix;
  final String sheetTitle;
  final String subtitle;
  final IconData icon;
}
