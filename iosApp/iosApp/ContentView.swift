import SwiftUI
import KlafAppKit

struct ComposeViewController: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        IosAppEntryPointKt.IosAppEntryPoint()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct ContentView: View {
    var body: some View {
        ComposeViewController()
            .ignoresSafeArea(.keyboard)
    }
}

#Preview {
    ContentView()
}
