import SwiftUI
import PresentationKit

struct ComposeViewController: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
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
