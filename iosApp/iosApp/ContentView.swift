import SwiftUI
import Shared

struct ContentView: View {
    var body: some View {
        ComposeView()
            // Let Compose manage safe-area and keyboard insets internally.
            .ignoresSafeArea(.all)
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
